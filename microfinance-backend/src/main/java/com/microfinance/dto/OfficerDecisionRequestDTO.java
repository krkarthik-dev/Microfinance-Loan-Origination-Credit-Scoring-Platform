package com.microfinance.dto;

import lombok.Data;

@Data
public class OfficerDecisionRequestDTO {
    /** APPROVE, REJECT, ESCALATE, REQUEST_INFO */
    private String decision;
    
    /** Specific reason code if REJECTED */
    private String rejectionReason;
    
    /** Internal notes for ESCALATED or REQUEST_INFO, or context for REJECTED */
    private String internalNotes;
}
