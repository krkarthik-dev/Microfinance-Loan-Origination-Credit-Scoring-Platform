package com.microfinance.dto;

import lombok.Data;

@Data
public class ChangePasswordRequestDTO {
    private String email;
    private String oldPassword;
    private String newPassword;
}
