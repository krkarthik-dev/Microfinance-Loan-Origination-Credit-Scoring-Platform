package com.microfinance.controller;

import com.microfinance.dto.DisbursementQueueItemDTO;
import com.microfinance.entity.AuditLog;
import com.microfinance.entity.DisbursementQueue;
import com.microfinance.entity.LoanApplication;
import com.microfinance.entity.SystemNotification;
import com.microfinance.entity.User;
import com.microfinance.enums.ApplicationStatus;
import com.microfinance.repository.AuditLogRepository;
import com.microfinance.repository.DisbursementQueueRepository;
import com.microfinance.repository.LoanApplicationRepository;
import com.microfinance.repository.SystemNotificationRepository;
import com.microfinance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/disbursements")
@RequiredArgsConstructor
public class AdminDisbursementController {

    private final DisbursementQueueRepository disbursementQueueRepository;
    private final LoanApplicationRepository loanApplicationRepository;
    private final SystemNotificationRepository systemNotificationRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @GetMapping("/queue")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<DisbursementQueueItemDTO>> getDisbursementQueue() {
        List<DisbursementQueue> queue = disbursementQueueRepository.findAll().stream()
                .filter(q -> "PENDING_DISBURSEMENT".equals(q.getStatus()))
                .collect(Collectors.toList());

        List<DisbursementQueueItemDTO> dtos = queue.stream().map(q -> {
            LoanApplication app = q.getLoanApplication();
            String applicantName = app.getApplicant().getUsername(); // Or fetch from UserProfile if needed, keeping simple here
            return DisbursementQueueItemDTO.builder()
                    .id(q.getId())
                    .applicationId(app.getId())
                    .applicationNumber(app.getApplicationNumber())
                    .applicantName(applicantName)
                    .approvedAmount(q.getApprovedAmount())
                    .status(q.getStatus())
                    .queuedAt(q.getQueuedAt())
                    .build();
        }).collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/{queueId}/disburse")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> markAsDisbursed(@PathVariable Long queueId) {
        Optional<DisbursementQueue> queueOpt = disbursementQueueRepository.findById(queueId);
        if (queueOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        DisbursementQueue queueItem = queueOpt.get();
        if (!"PENDING_DISBURSEMENT".equals(queueItem.getStatus())) {
            return ResponseEntity.badRequest().body("Item is not pending disbursement.");
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User admin = userRepository.findByEmail(auth.getName()).orElse(null);

        // 1. Mark Queue Item as COMPLETED
        queueItem.setStatus("COMPLETED");
        queueItem.setProcessedAt(LocalDateTime.now());
        disbursementQueueRepository.save(queueItem);

        // 2. Update Loan Application Status to ACTIVE_REPAYMENT
        LoanApplication app = queueItem.getLoanApplication();
        ApplicationStatus oldStatus = app.getStatus();
        app.setStatus(ApplicationStatus.ACTIVE_REPAYMENT);
        loanApplicationRepository.save(app);

        // 3. Audit Logging
        AuditLog audit = AuditLog.builder()
                .entityType("LOAN_APPLICATION")
                .entityId(app.getId())
                .action("MANUAL_DISBURSEMENT")
                .performedBy(admin)
                .oldValue(oldStatus.name())
                .newValue(ApplicationStatus.ACTIVE_REPAYMENT.name())
                .build();
        auditLogRepository.save(audit);

        // 4. Create System Notification for Borrower
        String message = String.format(
            "Your loan application %s has been disbursed offline. EMI repayment has started.", 
            app.getApplicationNumber()
        );
        SystemNotification notification = SystemNotification.builder()
                .user(app.getApplicant())
                .message(message)
                .linkUrl("/applicant/loan/" + app.getId() + "/tracking")
                .isRead(false)
                .build();
        systemNotificationRepository.save(notification);

        return ResponseEntity.ok().build();
    }
}
