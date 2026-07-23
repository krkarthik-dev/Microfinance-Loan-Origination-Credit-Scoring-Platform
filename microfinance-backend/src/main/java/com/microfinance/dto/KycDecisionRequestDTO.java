package com.microfinance.dto;

import lombok.Data;

@Data
public class KycDecisionRequestDTO {
    /** APPROVE or REJECT */
    private String decision;
    
    /** Reason if REJECT */
    private String rejectionReason;
}
