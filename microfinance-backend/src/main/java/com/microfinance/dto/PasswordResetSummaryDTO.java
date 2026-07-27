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
public class PasswordResetSummaryDTO {
    private Long id;
    private String requestId;
    private String firstName;
    private String lastName;
    private String email;
    private String status;
    private String tempPassword;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
}
