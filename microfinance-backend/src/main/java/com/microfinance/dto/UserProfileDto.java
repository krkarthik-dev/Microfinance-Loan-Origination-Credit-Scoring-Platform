package com.microfinance.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDto {

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotNull(message = "Date of birth is required")
    private LocalDate dateOfBirth;

    @NotBlank(message = "Gender is required")
    private String gender;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    private String phoneNumber;

    @NotBlank(message = "Address line 1 is required")
    private String addressLine1;

    private String addressLine2;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "State is required")
    private String state;

    @NotBlank(message = "Pincode is required")
    private String pincode;

    @Pattern(regexp = "^[A-Za-z]{5}[0-9]{4}[A-Za-z]{1}$", message = "Invalid PAN number format")
    private String panNumber;

    @Pattern(regexp = "^\\d{12}$", message = "Invalid Aadhaar number format")
    private String aadhaarNumber;

    @NotBlank(message = "Employment type is required")
    private String employmentType;

    @NotNull(message = "Monthly income is required")
    @Min(value = 0, message = "Monthly income must be a positive value")
    private BigDecimal monthlyIncome;
}
