package com.microfinance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCollectionRequestDto {
    private LocalDate paidDate;
    private String paymentMethod; // Cash, Cheque, Bank Transfer, UPI
    private String referenceNumber;
    private String notes;
}
