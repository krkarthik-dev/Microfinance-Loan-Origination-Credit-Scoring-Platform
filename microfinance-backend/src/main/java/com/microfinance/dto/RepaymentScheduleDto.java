package com.microfinance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepaymentScheduleDto {
    private String loanId;
    private String borrowerName;
    private BigDecimal principalAmount;
    private BigDecimal interestRate;
    private Integer tenureMonths;
    private BigDecimal nextEmiAmount;
    private LocalDate nextEmiDueDate;
    private BigDecimal totalOutstandingBalance;
    private String loanStatus;
    private List<EmiScheduleItemDto> schedule;
}
