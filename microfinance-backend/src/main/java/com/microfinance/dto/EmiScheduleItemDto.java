package com.microfinance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmiScheduleItemDto {
    private Long id;
    private Integer installmentNumber;
    private LocalDate dueDate;
    private BigDecimal principalComponent;
    private BigDecimal interestComponent;
    private BigDecimal totalEmi;
    private BigDecimal remainingBalance;
    private String status; // PENDING, PAID, OVERDUE
    private LocalDateTime paidDate;
    private String paymentMethod;
    private String referenceNumber;
    private String collectedByUsername;
    private Boolean isActionable; // True if this is the oldest unpaid EMI in sequence
}
