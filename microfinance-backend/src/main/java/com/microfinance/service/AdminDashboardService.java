package com.microfinance.service;

import com.microfinance.dto.AdminDashboardMetricsDTO;
import com.microfinance.dto.AuditLogDTO;
import com.microfinance.entity.AuditLog;
import com.microfinance.entity.User;
import com.microfinance.enums.ApplicationStatus;
import com.microfinance.enums.RiskTier;
import com.microfinance.repository.AuditLogRepository;
import com.microfinance.repository.CreditScoreRepository;
import com.microfinance.repository.LoanApplicationRepository;
import com.microfinance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final LoanApplicationRepository loanApplicationRepository;
    private final CreditScoreRepository creditScoreRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public AdminDashboardMetricsDTO getDashboardMetrics() {
        // 1. MTD Disbursed Amount (Active loans updated this month)
        LocalDateTime startOfMonth = YearMonth.now().atDay(1).atStartOfDay();
        BigDecimal mtdDisbursed = loanApplicationRepository.sumApprovedAmountByStatusInAndUpdatedAtAfter(
                List.of(ApplicationStatus.ACTIVE), startOfMonth);

        // 2. Total Pending Escalations
        long pendingEscalations = loanApplicationRepository.countByStatus(ApplicationStatus.ESCALATED);

        // 3. System Rejection Rate
        long totalApplications = loanApplicationRepository.count();
        long rejectedApplications = loanApplicationRepository.countByStatusIn(
                List.of(ApplicationStatus.REJECTED, ApplicationStatus.FINAL_REJECTED));
        
        double rejectionRate = 0.0;
        if (totalApplications > 0) {
            rejectionRate = Math.round(((double) rejectedApplications / totalApplications) * 1000.0) / 10.0; // 1 decimal place
        }

        // 4. Risk Distribution
        List<Object[]> riskCounts = creditScoreRepository.countByRiskTier();
        Map<String, Long> riskDistribution = riskCounts.stream()
                .collect(Collectors.toMap(
                        row -> ((RiskTier) row[0]).name(),
                        row -> (Long) row[1]
                ));

        // 5. Recent Audit Logs
        List<AuditLog> recentLogs = auditLogRepository.findTop10ByOrderByCreatedAtDesc();
        List<AuditLogDTO> auditLogDTOs = recentLogs.stream().map(this::mapToDTO).collect(Collectors.toList());

        return AdminDashboardMetricsDTO.builder()
                .mtdDisbursedAmount(mtdDisbursed)
                .pendingEscalations(pendingEscalations)
                .systemRejectionRate(rejectionRate)
                .totalApplications(totalApplications)
                .recentActivity(auditLogDTOs)
                .riskDistribution(riskDistribution)
                .build();
    }

    private AuditLogDTO mapToDTO(AuditLog log) {
        String performerName = "System";
        if (log.getPerformedBy() != null) {
            User user = userRepository.findById(log.getPerformedBy().getId()).orElse(null);
            if (user != null) {
                performerName = user.getUsername();
            }
        }
        
        String summary = String.format("%s performed %s on %s #%d", 
                performerName, log.getAction(), log.getEntityType(), log.getEntityId());

        return AuditLogDTO.builder()
                .id(log.getId())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .action(log.getAction())
                .performedBy(performerName)
                .createdAt(log.getCreatedAt())
                .summary(summary)
                .build();
    }
}
