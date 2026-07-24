package com.microfinance.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class NotificationDTO {
    private Long id;
    private String message;
    private String linkUrl;
    private boolean isRead;
    private LocalDateTime createdAt;
}
