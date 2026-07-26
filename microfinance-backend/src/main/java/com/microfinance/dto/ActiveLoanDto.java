package com.microfinance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActiveLoanDto {
    private String loanId;
    private BigDecimal principalAmount;
    private LocalDateTime disbursedDate;
    private BigDecimal currentOutstandingBalance;
    private Integer tenureMonths;
    private BigDecimal monthlyEmi;
    private BigDecimal interestRate;
    private String purpose;
}
