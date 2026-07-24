package com.microfinance.dto;

import com.microfinance.enums.RiskTier;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfficerApplicationSummaryDTO {

    private Long applicationId;
    private String applicantName;
    private String status;
    
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

    public OfficerApplicationSummaryDTO(String applicationNumber, String applicantFirstName,
                                        String applicantLastName, BigDecimal appliedAmount,
                                        Integer tenureMonths, String purpose,
                                        LocalDateTime submittedAt, Integer creditScore,
                                        RiskTier riskTier) {
        this.applicationNumber = applicationNumber;
        this.applicantFirstName = applicantFirstName;
        this.applicantLastName = applicantLastName;
        this.appliedAmount = appliedAmount;
        this.tenureMonths = tenureMonths;
        this.purpose = purpose;
        this.submittedAt = submittedAt;
        this.creditScore = creditScore;
        this.riskTier = riskTier;
    }
}
