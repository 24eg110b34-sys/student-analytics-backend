package com.spad.controller;

import com.spad.dto.Dtos.PredictRequest;
import com.spad.dto.Dtos.PredictionDto;
import com.spad.dto.Dtos.StudentDashboard;
import com.spad.dto.Dtos.SubjectDto;
import com.spad.exception.ApiException;
import com.spad.security.AuthUser;
import com.spad.service.DashboardService;
import com.spad.service.FacultyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Endpoints available to any signed-in user (students and faculty). */
@RestController
@RequestMapping("/api")
public class StudentController {

    private final DashboardService dashboardService;
    private final FacultyService facultyService;

    public StudentController(DashboardService dashboardService, FacultyService facultyService) {
        this.dashboardService = dashboardService;
        this.facultyService = facultyService;
    }

    @GetMapping("/me/dashboard")
    public StudentDashboard myDashboard(@AuthenticationPrincipal AuthUser user) {
        if (user.studentId() == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only students have a personal dashboard");
        }
        return dashboardService.studentDashboard(user.studentId());
    }

    @PostMapping("/predict")
    public PredictionDto predict(@Valid @RequestBody PredictRequest req) {
        return dashboardService.whatIf(req);
    }

    @GetMapping("/subjects")
    public List<SubjectDto> subjects() {
        return facultyService.subjects();
    }
}
