package com.microfinance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogDTO {
    private Long id;
    private String entityType;
    private Long entityId;
    private String action;
    private String performedBy;
    private LocalDateTime createdAt;
    
    // Extracted summary string for the frontend (e.g., "Officer John approved Loan #1024")
    private String summary;
}
