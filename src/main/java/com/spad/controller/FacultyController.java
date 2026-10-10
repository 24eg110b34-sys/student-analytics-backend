package com.spad.controller;

import com.spad.dto.Dtos.AttendanceRequest;
import com.spad.dto.Dtos.CreateStudentRequest;
import com.spad.dto.Dtos.FacultyOverview;
import com.spad.dto.Dtos.MarkRequest;
import com.spad.dto.Dtos.StudentDashboard;
import com.spad.dto.Dtos.StudentInfo;
import com.spad.dto.Dtos.StudentSummary;
import com.spad.dto.Dtos.StudyHoursRequest;
import com.spad.dto.Dtos.SubjectDto;
import com.spad.dto.Dtos.SubjectRequest;
import com.spad.service.DashboardService;
import com.spad.service.FacultyService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Faculty-only endpoints (enforced in SecurityConfig). */
@RestController
@RequestMapping("/api/faculty")
public class FacultyController {

    private final DashboardService dashboardService;
    private final FacultyService facultyService;

    public FacultyController(DashboardService dashboardService, FacultyService facultyService) {
        this.dashboardService = dashboardService;
        this.facultyService = facultyService;
    }

    @GetMapping("/overview")
    public FacultyOverview overview() {
        return dashboardService.facultyOverview();
    }

    @GetMapping("/students")
    public List<StudentSummary> students(@RequestParam(required = false) String q) {
        return dashboardService.students(q);
    }

    @PostMapping("/students")
    public StudentInfo createStudent(@Valid @RequestBody CreateStudentRequest req) {
        return facultyService.createStudent(req);
    }

    @DeleteMapping("/students/{id}")
    public ResponseEntity<Void> deleteStudent(@PathVariable Long id) {
        facultyService.deleteStudent(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/students/{id}/dashboard")
    public StudentDashboard studentDashboard(@PathVariable Long id) {
        return dashboardService.studentDashboard(id);
    }

    @PutMapping("/students/{id}/marks")
    public ResponseEntity<Void> saveMarks(@PathVariable Long id, @Valid @RequestBody MarkRequest req) {
        facultyService.saveMarks(id, req);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/students/{id}/attendance")
    public ResponseEntity<Void> saveAttendance(@PathVariable Long id, @Valid @RequestBody AttendanceRequest req) {
        facultyService.saveAttendance(id, req);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/students/{id}/study-hours")
    public ResponseEntity<Void> saveStudyHours(@PathVariable Long id, @Valid @RequestBody StudyHoursRequest req) {
        facultyService.saveStudyHours(id, req);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/subjects")
    public SubjectDto createSubject(@Valid @RequestBody SubjectRequest req) {
        return facultyService.createSubject(req);
    }
}
