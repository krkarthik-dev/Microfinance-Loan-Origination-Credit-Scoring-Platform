package com.microfinance.controller;

import com.microfinance.dto.OfficerApplicationSummaryDTO;
import com.microfinance.enums.ApplicationStatus;
import com.microfinance.repository.LoanApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/officer")
@RequiredArgsConstructor
public class OfficerDashboardController {

    private final LoanApplicationRepository loanApplicationRepository;

    /**
     * US16: Returns a list of all applications currently in the UNDER_REVIEW state.
     * The sorting logic by RiskTier/CreditScore will be handled by the frontend grid
     * as requested, or can be pre-sorted here.
     */
    @GetMapping("/applications/queue")
    @PreAuthorize("hasRole('OFFICER')")
    public ResponseEntity<List<OfficerApplicationSummaryDTO>> getUnderReviewApplications() {
        List<OfficerApplicationSummaryDTO> queue = loanApplicationRepository.findSummariesByStatus(ApplicationStatus.UNDER_REVIEW);
        return ResponseEntity.ok(queue);
    }
}
