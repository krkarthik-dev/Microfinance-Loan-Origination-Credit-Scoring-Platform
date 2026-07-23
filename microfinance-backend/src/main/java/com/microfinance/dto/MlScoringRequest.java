package com.microfinance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Data Transfer Object for serializing applicant data to JSON
 * for the Python ML Script.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MlScoringRequest {
    private Long applicationId;
    private Long applicantId;
    private BigDecimal appliedAmount;
    
    // Future expansion fields that the Python script expects defaults for currently
    private BigDecimal income;
    private Integer age;
    private BigDecimal existingDebt;
}
