package com.microfinance.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class DisbursementQueueItemDTO {
    private Long id;
    private Long applicationId;
    private String applicationNumber;
    private String applicantName;
    private BigDecimal approvedAmount;
    private String status;
    private LocalDateTime queuedAt;
}
