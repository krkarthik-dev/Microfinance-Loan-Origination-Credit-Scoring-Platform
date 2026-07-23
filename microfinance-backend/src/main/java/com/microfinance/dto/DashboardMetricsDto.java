package com.microfinance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for carrying dashboard summary metrics for a borrower.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardMetricsDto {
    private int activeLoans;
    private BigDecimal totalOutstanding;
    private int pendingApplications;
    private boolean profileComplete;
    private List<LoanActivityDto> recentActivity;
}
