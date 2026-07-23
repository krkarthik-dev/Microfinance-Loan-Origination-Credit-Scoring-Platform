package com.microfinance.controller;

import com.microfinance.dto.DashboardMetricsDto;
import com.microfinance.service.ApplicantDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for Applicant endpoints.
 * Secured by SecurityConfig to allow ROLE_APPLICANT.
 */
@RestController
@RequestMapping("/api/applicant")
@RequiredArgsConstructor
public class ApplicantController {

    private final ApplicantDashboardService applicantDashboardService;

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardMetricsDto> getApplicantDashboard(Authentication authentication) {
        String username = authentication.getName();
        DashboardMetricsDto metrics = applicantDashboardService.getDashboardMetrics(username);
        return ResponseEntity.ok(metrics);
    }
}
