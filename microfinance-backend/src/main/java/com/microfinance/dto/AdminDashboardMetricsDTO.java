package com.microfinance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardMetricsDTO {
    private BigDecimal mtdDisbursedAmount;
    private long pendingEscalations;
    private double systemRejectionRate; // Percentage e.g. 15.5 for 15.5%
    private long totalApplications;
    private List<AuditLogDTO> recentActivity;
    private Map<String, Long> riskDistribution;
}
