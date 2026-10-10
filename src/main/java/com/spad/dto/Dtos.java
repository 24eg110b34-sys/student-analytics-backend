package com.spad.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class Dtos {

    private Dtos() {}

    // ---------- auth ----------
    public record LoginRequest(
            @NotBlank(message = "Email is required") String email,
            @NotBlank(message = "Password is required") String password) {}

    public record UserDto(Long id, String name, String email, String role, Long studentId) {}

    public record AuthResponse(String token, UserDto user) {}

    // ---------- requests from faculty ----------
    public record CreateStudentRequest(
            @NotBlank(message = "Name is required") @Size(max = 100) String name,
            @NotBlank(message = "Email is required") @Email(message = "Enter a valid email address") String email,
            @NotBlank(message = "Roll number is required") @Size(max = 30) String rollNo,
            @NotBlank(message = "Department is required") @Size(max = 30) String department,
            @NotNull(message = "Semester is required")
            @Min(value = 1, message = "Semester must be 1-8") @Max(value = 8, message = "Semester must be 1-8") Integer semester,
            String password) {}

    public record MarkRequest(
            @NotNull(message = "Choose a subject") Long subjectId,
            @NotNull(message = "Assignment score is required")
            @DecimalMin(value = "0", message = "Scores must be 0-100") @DecimalMax(value = "100", message = "Scores must be 0-100") Double assignment,
            @NotNull(message = "Internal score is required")
            @DecimalMin(value = "0", message = "Scores must be 0-100") @DecimalMax(value = "100", message = "Scores must be 0-100") Double internal,
            @NotNull(message = "Exam score is required")
            @DecimalMin(value = "0", message = "Scores must be 0-100") @DecimalMax(value = "100", message = "Scores must be 0-100") Double exam) {}

    public record AttendanceRequest(
            @NotNull(message = "Choose a subject") Long subjectId,
            @NotNull(message = "Classes held is required") @Min(value = 1, message = "Classes held must be at least 1") Integer held,
            @NotNull(message = "Classes attended is required") @Min(value = 0, message = "Classes attended cannot be negative") Integer attended) {}

    public record StudyHoursRequest(
            @NotNull(message = "Semester is required")
            @Min(value = 1, message = "Semester must be 1-8") @Max(value = 8, message = "Semester must be 1-8") Integer semester,
            @NotNull(message = "Study hours are required")
            @DecimalMin(value = "0", message = "Study hours must be 0-60") @DecimalMax(value = "60", message = "Study hours must be 0-60") Double hoursPerWeek) {}

    public record SubjectRequest(
            @NotBlank(message = "Subject code is required") @Size(max = 20) String code,
            @NotBlank(message = "Subject name is required") @Size(max = 120) String name,
            @NotNull(message = "Credits are required")
            @Min(value = 1, message = "Credits must be 1-6") @Max(value = 6, message = "Credits must be 1-6") Integer credits,
            @NotNull(message = "Semester is required")
            @Min(value = 1, message = "Semester must be 1-8") @Max(value = 8, message = "Semester must be 1-8") Integer semester) {}

    public record PredictRequest(
            @NotNull(message = "Study hours are required")
            @DecimalMin(value = "0", message = "Study hours must be 0-60") @DecimalMax(value = "60", message = "Study hours must be 0-60") Double studyHours,
            @NotNull(message = "Attendance is required")
            @DecimalMin(value = "0", message = "Attendance must be 0-100") @DecimalMax(value = "100", message = "Attendance must be 0-100") Double attendance,
            @NotNull(message = "Assignment score is required")
            @DecimalMin(value = "0", message = "Assignment score must be 0-100") @DecimalMax(value = "100", message = "Assignment score must be 0-100") Double assignmentScore,
            @NotNull(message = "Previous CGPA is required")
            @DecimalMin(value = "0", message = "CGPA must be 0-10") @DecimalMax(value = "10", message = "CGPA must be 0-10") Double previousCgpa) {}

    // ---------- responses ----------
    public record SubjectDto(Long id, String code, String name, int credits, int semester) {}

    public record StudentInfo(Long id, String name, String email, String rollNo, String department, int semester) {}

    public record SemesterStat(int semester, double sgpa, double cgpa, double attendance,
                               double assignmentAvg, double studyHours) {}

    public record SubjectResult(Long subjectId, String code, String name, int credits,
                                double assignment, double internal, double exam, double total,
                                String grade, int gradePoint, double attendance) {}

    public record Highlight(String subject, double total) {}

    public record Coefficient(String name, double value) {}

    public record PredictionDto(int targetSemester, double predictedSgpa, String risk, String modelType,
                                double r2, int trainingRows, double intercept,
                                List<Coefficient> coefficients, List<String> advice) {}

    public record StudentDashboard(StudentInfo student, double cgpa, double latestSgpa, double attendance,
                                   double classAvgCgpa, int rank, int totalStudents, int latestSemester,
                                   List<SemesterStat> semesters, List<SubjectResult> subjects,
                                   List<Highlight> strongest, List<Highlight> weakest,
                                   PredictionDto prediction) {}

    public record StudentSummary(Long id, String name, String rollNo, String department, int semester,
                                 double cgpa, double attendance, Double predictedSgpa,
                                 String risk, String reason) {}

    public record DeptAverage(String department, double avgCgpa, int students) {}

    public record Bucket(String label, int count) {}

    public record TrendPoint(int semester, double avgSgpa, double avgAttendance) {}

    public record FacultyOverview(int totalStudents, double avgCgpa, double avgAttendance,
                                  int highRisk, int mediumRisk,
                                  List<Bucket> cgpaDistribution, List<DeptAverage> departments,
                                  List<TrendPoint> trend, List<StudentSummary> topPerformers,
                                  List<StudentSummary> needsSupport) {}
}
