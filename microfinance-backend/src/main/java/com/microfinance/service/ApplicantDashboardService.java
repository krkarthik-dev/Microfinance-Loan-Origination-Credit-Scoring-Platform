package com.microfinance.service;

import com.microfinance.dto.DashboardMetricsDto;
import com.microfinance.dto.LoanActivityDto;
import com.microfinance.entity.User;
import com.microfinance.entity.LoanApplication;
import com.microfinance.entity.UserProfile;
import com.microfinance.enums.ApplicationStatus;
import com.microfinance.repository.KycDocumentRepository;
import com.microfinance.repository.LoanApplicationRepository;
import com.microfinance.repository.UserProfileRepository;
import com.microfinance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicantDashboardService {

    private final LoanApplicationRepository loanApplicationRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final KycDocumentRepository kycDocumentRepository;

    /**
     * Calculates the borrower dashboard metrics.
     *
     * @param username the currently authenticated user's email/username
     * @return DashboardMetricsDto containing active loans, total outstanding, and pending counts.
     */
    @Transactional(readOnly = true)
    public DashboardMetricsDto getDashboardMetrics(String username) {
        log.info("Fetching dashboard metrics for user: {}", username);

        User applicant = userRepository.findByEmail(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        Long applicantId = applicant.getId();

        // 1. Pending Applications Count
        List<ApplicationStatus> pendingStatuses = Arrays.asList(
                ApplicationStatus.DRAFT,
                ApplicationStatus.SUBMITTED,
                ApplicationStatus.PENDING_KYC,
                ApplicationStatus.UNDER_REVIEW,
                ApplicationStatus.INFO_REQUESTED,
                ApplicationStatus.PENDING_MANAGER_APPROVAL,
                ApplicationStatus.CLOSING
        );
        int pendingApplications = loanApplicationRepository.countByApplicantIdAndStatusIn(applicantId, pendingStatuses);

        // 2. Active Loans Count
        List<ApplicationStatus> activeStatuses = Arrays.asList(
                ApplicationStatus.APPROVED,
                ApplicationStatus.ACTIVE_REPAYMENT
        );
        int activeLoans = loanApplicationRepository.countByApplicantIdAndStatusIn(applicantId, activeStatuses);

        // 3. Total Outstanding Balance
        BigDecimal totalOutstanding = loanApplicationRepository.sumApprovedAmountByApplicantIdAndStatusIn(applicantId, activeStatuses);

        // 4. Recent Activity
        List<LoanApplication> applications = loanApplicationRepository.findByApplicantIdOrderByCreatedAtDesc(applicantId);
        List<LoanActivityDto> recentActivity = applications.stream()
                .map(app -> LoanActivityDto.builder()
                        .loanId(app.getApplicationNumber())
                        .requestedAmount(app.getAppliedAmount())
                        .dateApplied(app.getSubmittedAt() != null ? app.getSubmittedAt() : app.getCreatedAt())
                        .status(app.getStatus())
                        .build())
                .collect(Collectors.toList());

        // 5. KYC Status Evaluation (US08 & US10)
        // Profile is complete if text profile is filled AND both PAN and Aadhaar are uploaded
        boolean hasTextProfile = userProfileRepository.findByUserId(applicantId)
                .map(profile -> profile.getPanNumber() != null && !profile.getPanNumber().trim().isEmpty() &&
                                profile.getAadhaarNumber() != null && !profile.getAadhaarNumber().trim().isEmpty() &&
                                profile.isKycVerified())
                .orElse(false);

        boolean hasPanDocument = kycDocumentRepository.findByUserIdAndDocumentType(applicantId, com.microfinance.enums.DocumentType.PAN).isPresent();
        boolean hasAadhaarDocument = kycDocumentRepository.findByUserIdAndDocumentType(applicantId, com.microfinance.enums.DocumentType.AADHAAR).isPresent();

        boolean profileComplete = hasTextProfile && hasPanDocument && hasAadhaarDocument;

        return DashboardMetricsDto.builder()
                .activeLoans(activeLoans)
                .totalOutstanding(totalOutstanding)
                .pendingApplications(pendingApplications)
                .profileComplete(profileComplete)
                .recentActivity(recentActivity)
                .build();
    }
}
