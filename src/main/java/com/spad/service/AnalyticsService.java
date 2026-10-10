package com.spad.service;

import com.spad.dto.Dtos.SemesterStat;
import com.spad.dto.Dtos.StudentInfo;
import com.spad.dto.Dtos.SubjectResult;
import com.spad.exception.ApiException;
import com.spad.model.Attendance;
import com.spad.model.Mark;
import com.spad.model.SemesterProfile;
import com.spad.model.Student;
import com.spad.repository.AttendanceRepository;
import com.spad.repository.MarkRepository;
import com.spad.repository.SemesterProfileRepository;
import com.spad.repository.StudentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** Loads student data and turns raw marks and attendance into SGPA, CGPA and per-semester statistics. */
@Service
public class AnalyticsService {

    public record StudentData(Student student, List<Mark> marks, List<Attendance> attendance,
                              List<SemesterProfile> profiles) {}

    public record Prepared(StudentData data, List<SemesterStat> stats) {
        public boolean hasData() {
            return !stats.isEmpty();
        }

        public SemesterStat latest() {
            return stats.get(stats.size() - 1);
        }

        public double cgpa() {
            return hasData() ? latest().cgpa() : 0;
        }
    }

    private final StudentRepository students;
    private final MarkRepository marks;
    private final AttendanceRepository attendance;
    private final SemesterProfileRepository profiles;

    public AnalyticsService(StudentRepository students, MarkRepository marks,
                            AttendanceRepository attendance, SemesterProfileRepository profiles) {
        this.students = students;
        this.marks = marks;
        this.attendance = attendance;
        this.profiles = profiles;
    }

    @Transactional(readOnly = true)
    public List<Prepared> prepareAll() {
        Map<Long, List<Mark>> marksByStudent = marks.findAllFetched().stream()
                .collect(Collectors.groupingBy(m -> m.getStudent().getId()));
        Map<Long, List<Attendance>> attByStudent = attendance.findAllFetched().stream()
                .collect(Collectors.groupingBy(a -> a.getStudent().getId()));
        Map<Long, List<SemesterProfile>> profByStudent = profiles.findAllFetched().stream()
                .collect(Collectors.groupingBy(p -> p.getStudent().getId()));

        List<Prepared> out = new ArrayList<>();
        for (Student s : students.findAllWithUser()) {
            StudentData d = new StudentData(s,
                    marksByStudent.getOrDefault(s.getId(), List.of()),
                    attByStudent.getOrDefault(s.getId(), List.of()),
                    profByStudent.getOrDefault(s.getId(), List.of()));
            out.add(new Prepared(d, semesterStats(d)));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public Prepared prepare(Long studentId) {
        Student s = students.findByIdWithUser(studentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Student not found"));
        StudentData d = new StudentData(s,
                marks.findByStudentIdFetched(studentId),
                attendance.findByStudentIdFetched(studentId),
                profiles.findByStudentId(studentId));
        return new Prepared(d, semesterStats(d));
    }

    public static StudentInfo info(Student s) {
        return new StudentInfo(s.getId(), s.getUser().getName(), s.getUser().getEmail(),
                s.getRollNo(), s.getDepartment(), s.getSemester());
    }

    /** SGPA, running CGPA, attendance, assignment average and study hours for every semester with marks. */
    public List<SemesterStat> semesterStats(StudentData d) {
        Map<Integer, List<Mark>> marksBySem = d.marks().stream()
                .collect(Collectors.groupingBy(m -> m.getSubject().getSemester(), TreeMap::new, Collectors.toList()));
        Map<Integer, List<Attendance>> attBySem = d.attendance().stream()
                .collect(Collectors.groupingBy(a -> a.getSubject().getSemester()));
        Map<Integer, Double> hours = new HashMap<>();
        for (SemesterProfile p : d.profiles()) {
            hours.put(p.getSemester(), p.getStudyHoursPerWeek());
        }

        List<SemesterStat> out = new ArrayList<>();
        double cumPoints = 0;
        double cumCredits = 0;
        for (Map.Entry<Integer, List<Mark>> e : marksBySem.entrySet()) {
            int sem = e.getKey();
            double points = 0;
            double credits = 0;
            double assignmentSum = 0;
            for (Mark m : e.getValue()) {
                int credit = m.getSubject().getCredits();
                points += credit * Grading.gradePoint(Grading.total(m));
                credits += credit;
                assignmentSum += m.getAssignmentScore();
            }
            cumPoints += points;
            cumCredits += credits;

            double held = 0;
            double attended = 0;
            for (Attendance a : attBySem.getOrDefault(sem, List.of())) {
                held += a.getClassesHeld();
                attended += a.getClassesAttended();
            }
            double sgpa = credits > 0 ? points / credits : 0;
            double cgpa = cumCredits > 0 ? cumPoints / cumCredits : 0;
            double attendancePct = held > 0 ? attended * 100.0 / held : 0;
            double assignmentAvg = assignmentSum / e.getValue().size();

            out.add(new SemesterStat(sem, Grading.round2(sgpa), Grading.round2(cgpa),
                    Grading.round1(attendancePct), Grading.round1(assignmentAvg),
                    Grading.round1(hours.getOrDefault(sem, 0.0))));
        }
        return out;
    }

    /** Subject-wise marks, grade and attendance for one semester. */
    public List<SubjectResult> subjectResults(StudentData d, int semester) {
        Map<Long, Attendance> attBySubject = new HashMap<>();
        for (Attendance a : d.attendance()) {
            if (a.getSubject().getSemester() == semester) {
                attBySubject.put(a.getSubject().getId(), a);
            }
        }
        return d.marks().stream()
                .filter(m -> m.getSubject().getSemester() == semester)
                .sorted(Comparator.comparing(m -> m.getSubject().getCode()))
                .map(m -> {
                    double total = Grading.total(m);
                    int gp = Grading.gradePoint(total);
                    Attendance a = attBySubject.get(m.getSubject().getId());
                    double pct = (a == null || a.getClassesHeld() == 0)
                            ? 0 : a.getClassesAttended() * 100.0 / a.getClassesHeld();
                    return new SubjectResult(m.getSubject().getId(), m.getSubject().getCode(),
                            m.getSubject().getName(), m.getSubject().getCredits(),
                            Grading.round1(m.getAssignmentScore()), Grading.round1(m.getInternalScore()),
                            Grading.round1(m.getExamScore()), Grading.round1(total),
                            Grading.letter(gp), gp, Grading.round1(pct));
                })
                .toList();
    }
}
