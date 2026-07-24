package com.microfinance.dto;

import com.microfinance.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffDTO {
    private Long id;
    private String email;
    private String username;
    private UserRole role;
    private boolean active;
}
