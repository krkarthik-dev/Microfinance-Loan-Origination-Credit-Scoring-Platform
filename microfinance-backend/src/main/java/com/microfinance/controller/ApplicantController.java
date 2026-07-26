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
    private final com.microfinance.service.RepaymentService repaymentService;

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardMetricsDto> getApplicantDashboard(Authentication authentication) {
        String username = authentication.getName();
        DashboardMetricsDto metrics = applicantDashboardService.getDashboardMetrics(username);
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/active-loans")
    public ResponseEntity<java.util.List<com.microfinance.dto.ActiveLoanDto>> getActiveLoans(Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(applicantDashboardService.getActiveLoans(username));
    }

    @GetMapping("/active-loans/{applicationNumber}/repayment-schedule")
    public ResponseEntity<?> getRepaymentSchedule(Authentication authentication, @org.springframework.web.bind.annotation.PathVariable String applicationNumber) {
        try {
            String username = authentication.getName();
            return ResponseEntity.ok(repaymentService.getBorrowerRepaymentSchedule(username, applicationNumber));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
