package com.microfinance.service;

import com.microfinance.dto.*;
import com.microfinance.entity.*;
import com.microfinance.enums.ApplicationStatus;
import com.microfinance.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RepaymentService {

    private final LoanApplicationRepository loanApplicationRepository;
    private final EmiInstallmentRepository emiInstallmentRepository;
    private final DisbursementQueueRepository disbursementQueueRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final SystemNotificationRepository systemNotificationRepository;
    private final UserProfileRepository userProfileRepository;

    @Transactional
    public List<EmiInstallment> ensureScheduleGenerated(LoanApplication app) {
        List<EmiInstallment> existing = emiInstallmentRepository.findByLoanApplicationIdOrderByInstallmentNumberAsc(app.getId());
        if (!existing.isEmpty()) {
            return existing;
        }

        log.info("Generating dynamic amortization schedule for loan {}", app.getApplicationNumber());
        BigDecimal principal = app.getAppliedAmount();
        BigDecimal annualRate = app.getLoanProduct() != null ? app.getLoanProduct().getInterestRatePa() : new BigDecimal("12.00");
        int tenure = app.getTenureMonths() > 0 ? app.getTenureMonths() : 12;

        BigDecimal emi = calculateEmi(principal, annualRate, tenure);

        LocalDate disbursedDate = disbursementQueueRepository.findByLoanApplicationId(app.getId())
                .map(q -> q.getProcessedAt() != null ? q.getProcessedAt().toLocalDate() : (q.getQueuedAt() != null ? q.getQueuedAt().toLocalDate() : LocalDate.now()))
                .orElse(app.getUpdatedAt() != null ? app.getUpdatedAt().toLocalDate() : LocalDate.now());

        BigDecimal currentBalance = principal;
        double monthlyRate = annualRate.doubleValue() / 12.0 / 100.0;

        List<EmiInstallment> installments = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = 1; i <= tenure; i++) {
            BigDecimal interest = currentBalance.multiply(BigDecimal.valueOf(monthlyRate)).setScale(2, RoundingMode.HALF_UP);
            BigDecimal principalComp = emi.subtract(interest);

            if (i == tenure || principalComp.compareTo(currentBalance) > 0) {
                principalComp = currentBalance;
                emi = principalComp.add(interest);
            }

            currentBalance = currentBalance.subtract(principalComp).max(BigDecimal.ZERO);
            LocalDate dueDate = disbursedDate.plusMonths(i);

            String status = "PENDING";
            LocalDateTime paidDate = null;

            // Simulate some historical payments for realistic demo
            if (dueDate.isBefore(today.minusDays(10))) {
                status = "PAID";
                paidDate = dueDate.atStartOfDay();
            } else if (dueDate.isBefore(today)) {
                status = "OVERDUE";
            }

            EmiInstallment inst = EmiInstallment.builder()
                    .loanApplication(app)
                    .installmentNumber(i)
                    .dueDate(dueDate)
                    .principalAmount(principalComp)
                    .interestAmount(interest)
                    .totalAmount(principalComp.add(interest))
                    .remainingBalance(currentBalance)
                    .status(status)
                    .paidDate(paidDate)
                    .paymentMethod(status.equals("PAID") ? "Bank Transfer" : null)
                    .referenceNumber(status.equals("PAID") ? "AUTO-" + System.currentTimeMillis() + "-" + i : null)
                    .build();

            installments.add(inst);
        }

        return emiInstallmentRepository.saveAll(installments);
    }

    private BigDecimal calculateEmi(BigDecimal principal, BigDecimal annualInterestRate, int months) {
        if (principal == null || annualInterestRate == null || months <= 0) return BigDecimal.ZERO;
        double p = principal.doubleValue();
        double r = annualInterestRate.doubleValue() / 12 / 100;
        if (r == 0) return principal.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
        double emi = (p * r * Math.pow(1 + r, months)) / (Math.pow(1 + r, months) - 1);
        return BigDecimal.valueOf(emi).setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional
    public RepaymentScheduleDto getBorrowerRepaymentSchedule(String username, String applicationNumber) {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        LoanApplication app = loanApplicationRepository.findByApplicationNumber(applicationNumber)
                .orElseThrow(() -> new IllegalArgumentException("Loan application not found"));

        if (!app.getApplicant().getId().equals(user.getId())) {
            throw new SecurityException("Unauthorized access to loan details");
        }

        if (app.getStatus() != ApplicationStatus.ACTIVE_REPAYMENT &&
            app.getStatus() != ApplicationStatus.COMPLETED &&
            app.getStatus() != ApplicationStatus.CLOSED_PAID_IN_FULL) {
            throw new IllegalStateException("Loan is not in active repayment or completed state");
        }

        List<EmiInstallment> installments = ensureScheduleGenerated(app);

        // Find oldest unpaid installment for sequential action enforcement
        EmiInstallment oldestUnpaid = installments.stream()
                .filter(i -> !i.getStatus().equals("PAID"))
                .min(Comparator.comparing(EmiInstallment::getInstallmentNumber))
                .orElse(null);

        List<EmiScheduleItemDto> scheduleDtos = installments.stream()
                .map(i -> EmiScheduleItemDto.builder()
                        .id(i.getId())
                        .installmentNumber(i.getInstallmentNumber())
                        .dueDate(i.getDueDate())
                        .principalComponent(i.getPrincipalAmount())
                        .interestComponent(i.getInterestAmount())
                        .totalEmi(i.getTotalAmount())
                        .remainingBalance(i.getRemainingBalance())
                        .status(i.getStatus())
                        .paidDate(i.getPaidDate())
                        .paymentMethod(i.getPaymentMethod())
                        .referenceNumber(i.getReferenceNumber())
                        .collectedByUsername(i.getCollectedBy() != null ? i.getCollectedBy().getEmail() : null)
                        .isActionable(oldestUnpaid != null && i.getId().equals(oldestUnpaid.getId()))
                        .build())
                        .collect(Collectors.toList());

        BigDecimal totalOutstanding = installments.stream()
                .filter(i -> !i.getStatus().equals("PAID"))
                .map(EmiInstallment::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal nextAmount = oldestUnpaid != null ? oldestUnpaid.getTotalAmount() : BigDecimal.ZERO;
        LocalDate nextDate = oldestUnpaid != null ? oldestUnpaid.getDueDate() : null;

        String borrowerName = getBorrowerFullName(app.getApplicant());

        return RepaymentScheduleDto.builder()
                .loanId(app.getApplicationNumber())
                .borrowerName(borrowerName)
                .principalAmount(app.getAppliedAmount())
                .interestRate(app.getLoanProduct() != null ? app.getLoanProduct().getInterestRatePa() : BigDecimal.ZERO)
                .tenureMonths(app.getTenureMonths())
                .nextEmiAmount(nextAmount)
                .nextEmiDueDate(nextDate)
                .totalOutstandingBalance(totalOutstanding)
                .loanStatus(app.getStatus().name())
                .schedule(scheduleDtos)
                .build();
    }

    @Transactional
    public List<OfficerDisbursedLoanDto> getOfficerDisbursedLoans(String officerUsername) {
        List<LoanApplication> activeLoans = loanApplicationRepository.findAll().stream()
                .filter(app -> app.getStatus() == ApplicationStatus.ACTIVE_REPAYMENT ||
                               app.getStatus() == ApplicationStatus.COMPLETED ||
                               app.getStatus() == ApplicationStatus.CLOSED_PAID_IN_FULL)
                .collect(Collectors.toList());

        List<OfficerDisbursedLoanDto> dtos = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (LoanApplication app : activeLoans) {
            List<EmiInstallment> installments = ensureScheduleGenerated(app);

            EmiInstallment oldestUnpaid = installments.stream()
                    .filter(i -> !i.getStatus().equals("PAID"))
                    .min(Comparator.comparing(EmiInstallment::getInstallmentNumber))
                    .orElse(null);

            BigDecimal totalOutstanding = installments.stream()
                    .filter(i -> !i.getStatus().equals("PAID"))
                    .map(EmiInstallment::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            String borrowerName = getBorrowerFullName(app.getApplicant());

            String statusBadge = "On Track";
            if (oldestUnpaid != null && (oldestUnpaid.getStatus().equals("OVERDUE") || oldestUnpaid.getDueDate().isBefore(today.plusDays(3)))) {
                statusBadge = "Payment Due";
            }
            if (oldestUnpaid == null) {
                statusBadge = "Paid in Full";
            }

            dtos.add(OfficerDisbursedLoanDto.builder()
                    .loanId(app.getApplicationNumber())
                    .borrowerName(borrowerName)
                    .totalDisbursed(app.getAppliedAmount())
                    .nextEmiDueDate(oldestUnpaid != null ? oldestUnpaid.getDueDate() : today)
                    .nextEmiAmount(oldestUnpaid != null ? oldestUnpaid.getTotalAmount() : BigDecimal.ZERO)
                    .status(statusBadge)
                    .totalOutstanding(totalOutstanding)
                    .build());
        }

        // Sort by Next EMI Due Date ASC (AC4 of Officer US56)
        dtos.sort(Comparator.comparing(OfficerDisbursedLoanDto::getNextEmiDueDate));
        return dtos;
    }

    @Transactional
    public RepaymentScheduleDto getOfficerRepaymentSchedule(String applicationNumber) {
        LoanApplication app = loanApplicationRepository.findByApplicationNumber(applicationNumber)
                .orElseThrow(() -> new IllegalArgumentException("Loan application not found"));
        return getBorrowerRepaymentSchedule(app.getApplicant().getEmail(), applicationNumber);
    }

    @Transactional
    public RepaymentScheduleDto markInstallmentAsPaid(String applicationNumber, Long installmentId, PaymentCollectionRequestDto request, String officerUsername) {
        User officer = userRepository.findByEmail(officerUsername)
                .orElseThrow(() -> new IllegalArgumentException("Officer not found"));

        LoanApplication app = loanApplicationRepository.findByApplicationNumber(applicationNumber)
                .orElseThrow(() -> new IllegalArgumentException("Loan application not found"));

        EmiInstallment targetInst = emiInstallmentRepository.findById(installmentId)
                .orElseThrow(() -> new IllegalArgumentException("Installment not found"));

        if (!targetInst.getLoanApplication().getId().equals(app.getId())) {
            throw new IllegalArgumentException("Installment does not belong to the specified loan");
        }

        if ("PAID".equals(targetInst.getStatus())) {
            throw new IllegalStateException("This installment has already been paid");
        }

        // Sequential payment enforcement (Officer US57 AC5)
        List<EmiInstallment> allInst = emiInstallmentRepository.findByLoanApplicationIdOrderByInstallmentNumberAsc(app.getId());
        for (EmiInstallment inst : allInst) {
            if (inst.getInstallmentNumber() < targetInst.getInstallmentNumber() && !"PAID".equals(inst.getStatus())) {
                throw new IllegalStateException("Sequential Payment Enforcement: Installment #" + targetInst.getInstallmentNumber() +
                        " cannot be paid while Installment #" + inst.getInstallmentNumber() + " is still unpaid.");
            }
        }

        targetInst.setStatus("PAID");
        targetInst.setPaidDate(request.getPaidDate() != null ? request.getPaidDate().atStartOfDay() : LocalDateTime.now());
        targetInst.setPaymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : "Cash");
        targetInst.setReferenceNumber(request.getReferenceNumber() != null ? request.getReferenceNumber() : "REF-" + System.currentTimeMillis());
        targetInst.setCollectedBy(officer);

        emiInstallmentRepository.save(targetInst);

        // Audit Logging (Officer US57 AC4)
        AuditLog audit = AuditLog.builder()
                .entityType("EMI_INSTALLMENT")
                .entityId(targetInst.getId())
                .action("EMI_COLLECTION")
                .performedBy(officer)
                .oldValue("PENDING/OVERDUE")
                .newValue("PAID - Method: " + targetInst.getPaymentMethod() + " | Ref: " + targetInst.getReferenceNumber())
                .build();
        auditLogRepository.save(audit);

        // US58 Terminal State Trigger check
        boolean allPaid = emiInstallmentRepository.findByLoanApplicationIdOrderByInstallmentNumberAsc(app.getId()).stream()
                .allMatch(i -> "PAID".equals(i.getStatus()));

        if (allPaid && app.getStatus() != ApplicationStatus.COMPLETED && app.getStatus() != ApplicationStatus.CLOSED_PAID_IN_FULL) {
            log.info("All EMIs paid for loan {}. Triggering US58 terminal closure to COMPLETED state.", app.getApplicationNumber());
            ApplicationStatus oldStatus = app.getStatus();
            app.setStatus(ApplicationStatus.COMPLETED);
            loanApplicationRepository.save(app);

            AuditLog closureAudit = AuditLog.builder()
                    .entityType("LOAN_APPLICATION")
                    .entityId(app.getId())
                    .action("LOAN_COMPLETED")
                    .performedBy(officer)
                    .oldValue(oldStatus.name())
                    .newValue("COMPLETED - Fully Paid")
                    .build();
            auditLogRepository.save(closureAudit);

            SystemNotification notification = SystemNotification.builder()
                    .user(app.getApplicant())
                    .message("Congratulations! Your loan " + app.getApplicationNumber() + " is fully paid. Your No Dues Certificate is now available.")
                    .linkUrl("/applicant/active-loans/" + app.getApplicationNumber())
                    .isRead(false)
                    .build();
            systemNotificationRepository.save(notification);
        }

        return getBorrowerRepaymentSchedule(app.getApplicant().getEmail(), applicationNumber);
    }

    private String getBorrowerFullName(User applicant) {
        if (applicant == null) return "Borrower";
        return userProfileRepository.findByUserId(applicant.getId())
                .map(p -> (p.getFirstName() != null ? p.getFirstName() : "") + " " + (p.getLastName() != null ? p.getLastName() : ""))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .orElse(applicant.getEmail());
    }
}
