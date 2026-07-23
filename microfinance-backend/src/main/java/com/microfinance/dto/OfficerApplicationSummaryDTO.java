package com.microfinance.dto;

import com.microfinance.enums.RiskTier;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OfficerApplicationSummaryDTO {
    
    private String applicationNumber;
    private String applicantFirstName;
    private String applicantLastName;
    private BigDecimal appliedAmount;
    private Integer tenureMonths;
    private String purpose;
    private LocalDateTime submittedAt;
    
    // ML Score Data
    private Integer creditScore;
    private RiskTier riskTier;
    
}
