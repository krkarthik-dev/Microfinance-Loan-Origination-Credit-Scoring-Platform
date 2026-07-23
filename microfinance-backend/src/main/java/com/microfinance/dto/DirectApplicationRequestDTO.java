package com.microfinance.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class DirectApplicationRequestDTO {
    private String firstName;
    private String lastName;
    private String email;
    private String temporaryPassword;
    private LocalDate dateOfBirth;
    private String employmentType;
    private BigDecimal monthlyIncome;
}
