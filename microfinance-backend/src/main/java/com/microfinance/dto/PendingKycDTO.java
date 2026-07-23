package com.microfinance.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PendingKycDTO {
    private Long userId;
    private String fullName;
    private String email;
    private String panNumber;
    private String aadhaarNumber;
    private LocalDateTime profileCreatedAt;
    
    // IDs for the documents to view
    private Long panDocumentId;
    private Long aadhaarDocumentId;
}
