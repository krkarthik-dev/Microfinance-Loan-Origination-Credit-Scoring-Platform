package com.microfinance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.microfinance.enums.UserRole;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffCreationRequestDTO {
    private String email;
    private String username; // Or we can generate this based on email
    private UserRole role;
}
