package com.microfinance.dto;

import com.microfinance.enums.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO representing a single historical loan application for the Recent Activity grid.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanActivityDto {
    private String loanId;
    private BigDecimal requestedAmount;
    private LocalDateTime dateApplied;
    private ApplicationStatus status;
}
