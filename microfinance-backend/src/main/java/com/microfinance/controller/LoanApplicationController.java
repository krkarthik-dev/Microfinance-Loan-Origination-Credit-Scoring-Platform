package com.microfinance.controller;

import com.microfinance.service.LoanApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Controller exposing endpoints for loan application submission.
 * Secured under /api/applicant/** (ROLE_APPLICANT) configured in SecurityConfig.
 */
@RestController
@RequestMapping("/api/applicant/loans")
@RequiredArgsConstructor
public class LoanApplicationController {

    private final LoanApplicationService loanApplicationService;

    /**
     * AC4: Non-blocking endpoint.
     * Receives a submission request, fires an event, and returns 202 Accepted immediately.
     */
    @PostMapping("/{applicationId}/submit")
    public ResponseEntity<Map<String, String>> submitLoanApplication(
            @PathVariable Long applicationId,
            @RequestParam Long applicantId,
            @RequestParam BigDecimal amount) {

        // The service triggers an async event and returns immediately
        loanApplicationService.submitApplication(applicationId, applicantId, amount);

        return ResponseEntity.accepted().body(
                Map.of("message", "Application submitted successfully. Risk assessment is running in the background.")
        );
    }
}
