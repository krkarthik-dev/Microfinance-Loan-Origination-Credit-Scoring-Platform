package com.microfinance.dto;

import com.microfinance.enums.RiskTier;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfficerApplicationDetailDTO {
    
    // Application Basic
    private Long applicationId;
    private String applicationNumber;
    private LocalDateTime submittedAt;
    
    // Profile Data (Section A)
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private String gender;
    private String phone;
    private String email;
    private String address; // Combining line1 + line2 + city + state + zip for display
    private String employmentType;
    private BigDecimal monthlyIncome;
    private String panNumber;
    private String aadhaarNumber;
    
    // Loan Data (Section B)
    private BigDecimal appliedAmount;
    private Integer tenureMonths;
    private BigDecimal interestRate;
    private BigDecimal monthlyEmi;
    private BigDecimal totalPayable;
    private String purpose;
    
    // Guarantor Data
    private String guarantorName;
    private String guarantorAddress;
    private String guarantorCity;
    private String guarantorZip;
    private String guarantorAadhaar;
    private String guarantorPan;

    // ML Data
    private Integer creditScore;
    private RiskTier riskTier;

    // Documents
    private Long panDocumentId;
    private Long aadhaarDocumentId;
    private Long incomeDocumentId;
    private Long photoDocumentId;
    private Long guarantorIdDocumentId;
    private List<DocumentSummaryDTO> otherDocuments;
    
    @Data
    @AllArgsConstructor
    public static class DocumentSummaryDTO {
        private Long id;
        private String fileName;
        private String documentType;
    }
}
