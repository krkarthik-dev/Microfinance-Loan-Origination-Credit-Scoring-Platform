package com.microfinance.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * AC2: Native Spring ApplicationEvent payload representing a newly submitted loan.
 * 
 * This object is passed seamlessly from the main HTTP thread to the 
 * background async thread via the ApplicationEventPublisher.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanSubmittedEvent {
    
    private Long applicationId;
    private Long applicantId;
    private BigDecimal appliedAmount;
    
}
