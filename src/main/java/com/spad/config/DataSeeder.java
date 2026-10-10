package com.spad.config;

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * On the first start (empty database) this creates a faculty login, six semesters of subjects and
 * 24 students with four semesters of marks, attendance and study hours, so every chart and the
 * prediction model have data straight away. Turn it off with SEED_DEMO_DATA=false.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final String[][] SUBJECTS = {
            {"MA101", "Engineering Mathematics I", "4", "1"},
            {"PH101", "Engineering Physics", "3", "1"},
            {"CS101", "Programming in C", "4", "1"},
            {"EE101", "Basic Electrical Engineering", "3", "1"},
            {"HS101", "Communication Skills", "2", "1"},
            {"MA102", "Engineering Mathematics II", "4", "2"},
            {"CH101", "Engineering Chemistry", "3", "2"},
            {"CS102", "Data Structures", "4", "2"},
            {"ME101", "Engineering Graphics", "3", "2"},
            {"CS103", "C Programming Lab", "2", "2"},
            {"MA201", "Discrete Mathematics", "4", "3"},
            {"CS201", "Object Oriented Programming", "4", "3"},
            {"CS202", "Digital Logic Design", "3", "3"},
            {"CS203", "Computer Organization", "3", "3"},
            {"CS204", "Data Structures Lab", "2", "3"},
            {"MA202", "Probability and Statistics", "4", "4"},
            {"CS205", "Database Management Systems", "4", "4"},
            {"CS206", "Operating Systems", "4", "4"},
            {"CS207", "Design and Analysis of Algorithms", "3", "4"},
            {"CS208", "DBMS Lab", "2", "4"},
            {"CS301", "Computer Networks", "4", "5"},
            {"CS302", "Software Engineering", "3", "5"},
            {"CS303", "Theory of Computation", "4", "5"},
            {"CS304", "Web Technologies", "3", "5"},
            {"CS305", "Networks Lab", "2", "5"},
            {"CS306", "Machine Learning", "4", "6"},
            {"CS307", "Compiler Design", "4", "6"},
            {"CS308", "Cloud Computing", "3", "6"},
            {"CS309", "Professional Elective I", "3", "6"},
            {"CS310", "Mini Project", "2", "6"},
    };

    private static final String[] FIRST_NAMES = {
            "Aarav", "Diya", "Rohan", "Ananya", "Vihaan", "Isha", "Karthik", "Meera", "Arjun", "Sneha",
            "Rahul", "Priya", "Aditya", "Kavya", "Siddharth", "Neha", "Varun", "Pooja", "Manish", "Lakshmi",
            "Harsh", "Tanvi", "Nikhil", "Divya"};
    private static final String[] LAST_NAMES = {
            "Sharma", "Reddy", "Iyer", "Nair", "Patel", "Gupta", "Menon", "Rao", "Singh", "Das", "Kumar", "Joshi"};
    private static final String[] DEPARTMENTS = {"CSE", "ECE", "IT", "MECH"};
    private static final int COMPLETED_SEMESTERS = 4;

    private final boolean enabled;
    private final UserRepository users;
    private final StudentRepository students;
    private final SubjectRepository subjects;
    private final MarkRepository marks;
    private final AttendanceRepository attendance;
    private final SemesterProfileRepository profiles;
    private final PasswordEncoder encoder;

    public DataSeeder(@Value("${app.seed:true}") boolean enabled, UserRepository users, StudentRepository students,
                      SubjectRepository subjects, MarkRepository marks, AttendanceRepository attendance,
                      SemesterProfileRepository profiles, PasswordEncoder encoder) {
        this.enabled = enabled;
        this.users = users;
        this.students = students;
        this.subjects = subjects;
        this.marks = marks;
        this.attendance = attendance;
        this.profiles = profiles;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        if (!enabled || users.count() > 0) {
            return;
        }
        Random rnd = new Random(42);

        User faculty = new User();
        faculty.setName("Dr. Meera Nair");
        faculty.setEmail("faculty@demo.com");
        faculty.setPasswordHash(encoder.encode("faculty123"));
        faculty.setRole(Role.FACULTY);
        users.save(faculty);

        Map<Integer, List<Subject>> subjectsBySem = new HashMap<>();
        for (String[] row : SUBJECTS) {
            Subject s = new Subject();
            s.setCode(row[0]);
            s.setName(row[1]);
            s.setCredits(Integer.parseInt(row[2]));
            s.setSemester(Integer.parseInt(row[3]));
            subjects.save(s);
            subjectsBySem.computeIfAbsent(s.getSemester(), k -> new ArrayList<>()).add(s);
        }

        String studentHash = encoder.encode(com.spad.service.FacultyService.DEFAULT_PASSWORD);
        List<Mark> markRows = new ArrayList<>();
        List<Attendance> attendanceRows = new ArrayList<>();
        List<SemesterProfile> profileRows = new ArrayList<>();

        for (int i = 0; i < FIRST_NAMES.length; i++) {
            String dept = DEPARTMENTS[i % DEPARTMENTS.length];

            User user = new User();
            user.setName(FIRST_NAMES[i] + " " + LAST_NAMES[i % LAST_NAMES.length]);
            user.setEmail(FIRST_NAMES[i].toLowerCase() + "@demo.com");
            user.setPasswordHash(studentHash);
            user.setRole(Role.STUDENT);
            users.save(user);

            Student student = new Student();
            student.setUser(user);
            student.setRollNo(String.format("21%s%03d", dept, i + 1));
            student.setDepartment(dept);
            student.setSemester(COMPLETED_SEMESTERS);
            students.save(student);

            double ability = 0.30 + 0.65 * rnd.nextDouble();
            double drift = (rnd.nextDouble() - 0.4) * 0.05;
            for (int sem = 1; sem <= COMPLETED_SEMESTERS; sem++) {
                double a = clamp(ability + drift * (sem - 1) + rnd.nextGaussian() * 0.03, 0.2, 0.98);
                double study = clamp(3 + 17 * a + rnd.nextGaussian() * 2.5, 2, 30);
                double att = clamp(50 + 45 * a + rnd.nextGaussian() * 5, 40, 100);

                SemesterProfile profile = new SemesterProfile();
                profile.setStudent(student);
                profile.setSemester(sem);
                profile.setStudyHoursPerWeek(Math.round(study * 10) / 10.0);
                profileRows.add(profile);

                for (Subject subject : subjectsBySem.get(sem)) {
                    Mark m = new Mark();
                    m.setStudent(student);
                    m.setSubject(subject);
                    m.setAssignmentScore(round1(clamp(30 + 62 * a + rnd.nextGaussian() * 7, 20, 100)));
                    m.setInternalScore(round1(clamp(25 + 65 * a + rnd.nextGaussian() * 7, 15, 100)));
                    m.setExamScore(round1(clamp(18 + 55 * a + 0.9 * study + 0.1 * (att - 70)
                            + rnd.nextGaussian() * 6, 15, 100)));
                    markRows.add(m);

                    int held = 38 + rnd.nextInt(9);
                    double pct = clamp(att + rnd.nextGaussian() * 4, 30, 100);
                    Attendance at = new Attendance();
                    at.setStudent(student);
                    at.setSubject(subject);
                    at.setClassesHeld(held);
                    at.setClassesAttended((int) Math.round(held * pct / 100.0));
                    attendanceRows.add(at);
                }
            }
        }
        profiles.saveAll(profileRows);
        marks.saveAll(markRows);
        attendance.saveAll(attendanceRows);
        System.out.println("Demo data loaded. Faculty login: faculty@demo.com / faculty123. "
                + "Student login: aarav@demo.com / student123");
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
