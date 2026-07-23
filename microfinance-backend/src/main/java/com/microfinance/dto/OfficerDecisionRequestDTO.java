package com.microfinance.dto;

import lombok.Data;

@Data
public class OfficerDecisionRequestDTO {
    /** APPROVE, REJECT, ESCALATE */
    private String decision;
    
    /** Specific reason code if REJECTED */
    private String rejectionReason;
    
    /** Internal notes for ESCALATED or context for REJECTED */
    private String internalNotes;
}
