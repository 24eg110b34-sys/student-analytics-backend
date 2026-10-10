package com.spad.service;

import com.spad.dto.Dtos.AttendanceRequest;
import com.spad.dto.Dtos.CreateStudentRequest;
import com.spad.dto.Dtos.MarkRequest;
import com.spad.dto.Dtos.StudentInfo;
import com.spad.dto.Dtos.StudyHoursRequest;
import com.spad.dto.Dtos.SubjectDto;
import com.spad.dto.Dtos.SubjectRequest;
import com.spad.exception.ApiException;
import com.spad.model.Attendance;
import com.spad.model.Mark;
import com.spad.model.Role;
import com.spad.model.SemesterProfile;
import com.spad.model.Student;
import com.spad.model.Subject;
import com.spad.model.User;
import com.spad.repository.AttendanceRepository;
import com.spad.repository.MarkRepository;
import com.spad.repository.SemesterProfileRepository;
import com.spad.repository.StudentRepository;
import com.spad.repository.SubjectRepository;
import com.spad.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Everything faculty can change: students, subjects, marks, attendance and study hours. */
@Service
public class FacultyService {

    public static final String DEFAULT_PASSWORD = "student123";

    private final UserRepository users;
    private final StudentRepository students;
    private final SubjectRepository subjects;
    private final MarkRepository marks;
    private final AttendanceRepository attendance;
    private final SemesterProfileRepository profiles;
    private final PasswordEncoder encoder;

    public FacultyService(UserRepository users, StudentRepository students, SubjectRepository subjects,
                          MarkRepository marks, AttendanceRepository attendance,
                          SemesterProfileRepository profiles, PasswordEncoder encoder) {
        this.users = users;
        this.students = students;
        this.subjects = subjects;
        this.marks = marks;
        this.attendance = attendance;
        this.profiles = profiles;
        this.encoder = encoder;
    }

    @Transactional
    public StudentInfo createStudent(CreateStudentRequest r) {
        String email = r.email().trim().toLowerCase();
        String rollNo = r.rollNo().trim().toUpperCase();
        if (users.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "A user with this email already exists");
        }
        if (students.existsByRollNo(rollNo)) {
            throw new ApiException(HttpStatus.CONFLICT, "A student with this roll number already exists");
        }
        String password = (r.password() == null || r.password().isBlank()) ? DEFAULT_PASSWORD : r.password();
        if (password.length() < 6) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Password must be at least 6 characters");
        }
        User user = new User();
        user.setName(r.name().trim());
        user.setEmail(email);
        user.setPasswordHash(encoder.encode(password));
        user.setRole(Role.STUDENT);
        users.save(user);

        Student student = new Student();
        student.setUser(user);
        student.setRollNo(rollNo);
        student.setDepartment(r.department().trim().toUpperCase());
        student.setSemester(r.semester());
        students.save(student);
        return AnalyticsService.info(student);
    }

    @Transactional
    public void deleteStudent(Long id) {
        Student student = findStudent(id);
        marks.deleteByStudentId(id);
        attendance.deleteByStudentId(id);
        profiles.deleteByStudentId(id);
        User user = student.getUser();
        students.delete(student);
        users.delete(user);
    }

    @Transactional
    public void saveMarks(Long studentId, MarkRequest r) {
        Student student = findStudent(studentId);
        Subject subject = findSubject(r.subjectId());
        Mark mark = marks.findByStudentIdAndSubjectId(studentId, subject.getId()).orElseGet(Mark::new);
        mark.setStudent(student);
        mark.setSubject(subject);
        mark.setAssignmentScore(r.assignment());
        mark.setInternalScore(r.internal());
        mark.setExamScore(r.exam());
        marks.save(mark);
        if (subject.getSemester() > student.getSemester()) {
            student.setSemester(subject.getSemester());
        }
    }

    @Transactional
    public void saveAttendance(Long studentId, AttendanceRequest r) {
        if (r.attended() > r.held()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Classes attended cannot be more than classes held");
        }
        Student student = findStudent(studentId);
        Subject subject = findSubject(r.subjectId());
        Attendance record = attendance.findByStudentIdAndSubjectId(studentId, subject.getId()).orElseGet(Attendance::new);
        record.setStudent(student);
        record.setSubject(subject);
        record.setClassesHeld(r.held());
        record.setClassesAttended(r.attended());
        attendance.save(record);
    }

    @Transactional
    public void saveStudyHours(Long studentId, StudyHoursRequest r) {
        Student student = findStudent(studentId);
        SemesterProfile profile = profiles.findByStudentIdAndSemester(studentId, r.semester())
                .orElseGet(SemesterProfile::new);
        profile.setStudent(student);
        profile.setSemester(r.semester());
        profile.setStudyHoursPerWeek(r.hoursPerWeek());
        profiles.save(profile);
    }

    @Transactional(readOnly = true)
    public List<SubjectDto> subjects() {
        return subjects.findAllByOrderBySemesterAscCodeAsc().stream()
                .map(s -> new SubjectDto(s.getId(), s.getCode(), s.getName(), s.getCredits(), s.getSemester()))
                .toList();
    }

    @Transactional
    public SubjectDto createSubject(SubjectRequest r) {
        String code = r.code().trim().toUpperCase();
        if (subjects.existsByCode(code)) {
            throw new ApiException(HttpStatus.CONFLICT, "A subject with this code already exists");
        }
        Subject s = new Subject();
        s.setCode(code);
        s.setName(r.name().trim());
        s.setCredits(r.credits());
        s.setSemester(r.semester());
        subjects.save(s);
        return new SubjectDto(s.getId(), s.getCode(), s.getName(), s.getCredits(), s.getSemester());
    }

    private Student findStudent(Long id) {
        return students.findByIdWithUser(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Student not found"));
    }

    private Subject findSubject(Long id) {
        return subjects.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Subject not found"));
    }
}
