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
    private final com.microfinance.repository.DisbursementQueueRepository disbursementQueueRepository;

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

        // 5. KYC Status Evaluation (US08, US10 & US67)
        UserProfile userProfile = userProfileRepository.findByUserId(applicantId).orElse(null);
        boolean kycVerified = userProfile != null && userProfile.isKycVerified();
        String kycStatus = userProfile != null && userProfile.getKycStatus() != null ? userProfile.getKycStatus() : "MISSING";

        boolean hasPanDocument = kycDocumentRepository.findByUserIdAndDocumentType(applicantId, com.microfinance.enums.DocumentType.PAN).isPresent();
        boolean hasAadhaarDocument = kycDocumentRepository.findByUserIdAndDocumentType(applicantId, com.microfinance.enums.DocumentType.AADHAAR).isPresent();

        if (userProfile != null && !kycVerified && (hasPanDocument || hasAadhaarDocument) && "MISSING".equals(kycStatus)) {
            kycStatus = "PENDING";
        }

        boolean hasTextProfile = userProfile != null &&
                userProfile.getPanNumber() != null && !userProfile.getPanNumber().trim().isEmpty() &&
                userProfile.getAadhaarNumber() != null && !userProfile.getAadhaarNumber().trim().isEmpty() &&
                kycVerified;

        boolean profileComplete = hasTextProfile && hasPanDocument && hasAadhaarDocument;

        return DashboardMetricsDto.builder()
                .activeLoans(activeLoans)
                .totalOutstanding(totalOutstanding)
                .pendingApplications(pendingApplications)
                .profileComplete(profileComplete)
                .kycVerified(kycVerified)
                .kycStatus(kycStatus)
                .recentActivity(recentActivity)
                .build();
    }

    /**
     * US56: Retrieves all active loans for the authenticated borrower.
     */
    @Transactional(readOnly = true)
    public List<com.microfinance.dto.ActiveLoanDto> getActiveLoans(String username) {
        log.info("Fetching active loans for user: {}", username);
        User applicant = userRepository.findByEmail(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        Long applicantId = applicant.getId();

        List<LoanApplication> applications = loanApplicationRepository.findByApplicantIdOrderByCreatedAtDesc(applicantId);
        return applications.stream()
                .filter(app -> app.getStatus() == ApplicationStatus.ACTIVE_REPAYMENT ||
                               app.getStatus() == ApplicationStatus.COMPLETED ||
                               app.getStatus() == ApplicationStatus.CLOSED_PAID_IN_FULL)
                .map(app -> {
                    BigDecimal principal = app.getApprovedAmount() != null ? app.getApprovedAmount() : app.getAppliedAmount();
                    BigDecimal interestRate = app.getLoanProduct() != null && app.getLoanProduct().getInterestRatePa() != null 
                            ? app.getLoanProduct().getInterestRatePa() : new BigDecimal("12.0");
                    int tenure = app.getTenureMonths() != null ? app.getTenureMonths() : 12;
                    
                    double p = principal.doubleValue();
                    double r = interestRate.doubleValue() / 12 / 100;
                    BigDecimal monthlyEmi = BigDecimal.ZERO;
                    BigDecimal totalPayable = principal;
                    if (r > 0 && tenure > 0) {
                        double emiVal = (p * r * Math.pow(1 + r, tenure)) / (Math.pow(1 + r, tenure) - 1);
                        monthlyEmi = BigDecimal.valueOf(emiVal).setScale(2, java.math.RoundingMode.HALF_UP);
                        totalPayable = monthlyEmi.multiply(BigDecimal.valueOf(tenure));
                    } else if (tenure > 0) {
                        monthlyEmi = principal.divide(BigDecimal.valueOf(tenure), 2, java.math.RoundingMode.HALF_UP);
                    }

                    java.time.LocalDateTime disbursedDate = disbursementQueueRepository.findAll().stream()
                            .filter(q -> q.getLoanApplication() != null && q.getLoanApplication().getId().equals(app.getId()) && "COMPLETED".equals(q.getStatus()))
                            .map(com.microfinance.entity.DisbursementQueue::getProcessedAt)
                            .filter(java.util.Objects::nonNull)
                            .findFirst()
                            .orElse(app.getUpdatedAt() != null ? app.getUpdatedAt() : (app.getSubmittedAt() != null ? app.getSubmittedAt() : app.getCreatedAt()));

                    return com.microfinance.dto.ActiveLoanDto.builder()
                            .loanId(app.getApplicationNumber())
                            .principalAmount(principal)
                            .disbursedDate(disbursedDate)
                            .currentOutstandingBalance(totalPayable)
                            .tenureMonths(tenure)
                            .monthlyEmi(monthlyEmi)
                            .interestRate(interestRate)
                            .purpose(app.getPurpose())
                            .build();
                })
                .collect(Collectors.toList());
    }
}
