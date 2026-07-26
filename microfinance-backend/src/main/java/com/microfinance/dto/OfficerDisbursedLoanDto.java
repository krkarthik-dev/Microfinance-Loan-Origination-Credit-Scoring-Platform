package com.microfinance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfficerDisbursedLoanDto {
    private String loanId;
    private String borrowerName;
    private BigDecimal totalDisbursed;
    private LocalDate nextEmiDueDate;
    private BigDecimal nextEmiAmount;
    private String status; // 'On Track' or 'Payment Due'
    private BigDecimal totalOutstanding;
}
