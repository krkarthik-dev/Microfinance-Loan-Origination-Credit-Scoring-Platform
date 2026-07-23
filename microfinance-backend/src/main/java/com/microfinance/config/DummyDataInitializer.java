package com.microfinance.config;

import com.microfinance.entity.LoanApplication;
import com.microfinance.entity.UserProfile;
import com.microfinance.entity.User;
import com.microfinance.enums.ApplicationStatus;
import com.microfinance.enums.UserRole;
import com.microfinance.repository.LoanApplicationRepository;
import com.microfinance.repository.UserProfileRepository;
import com.microfinance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Initializes dummy data for testing purposes.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DummyDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final UserProfileRepository profileRepository;
    private final LoanApplicationRepository loanApplicationRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        String testEmail = "test@example.com";
        
        if (userRepository.findByEmail(testEmail).isEmpty()) {
            log.info("Creating dummy test user: {}", testEmail);
            
            // Create User
            User dummyUser = User.builder()
                    .username(testEmail)
                    .email(testEmail)
                    .passwordHash(passwordEncoder.encode("test123"))
                    .role(UserRole.ROLE_APPLICANT)
                    .active(true)
                    .build();
            userRepository.save(dummyUser);

            // Create Profile
            UserProfile dummyProfile = UserProfile.builder()
                    .user(dummyUser)
                    .firstName("Test")
                    .lastName("Borrower")
                    .gender("Male")
                    .phoneNumber("+1-555-0199")
                    .dateOfBirth(LocalDate.of(1990, 1, 1))
                    .panNumber("TX9998888")
                    .aadhaarNumber("123456789012")
                    .addressLine1("123 Fake Street")
                    .city("Dummy City")
                    .state("Dummy State")
                    .pincode("12345")
                    .build();
            profileRepository.save(dummyProfile);

            // Create some dummy active loans
            LoanApplication activeLoan1 = LoanApplication.builder()
                    .applicationNumber("APP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .applicant(dummyUser)
                    .status(ApplicationStatus.FINAL_APPROVED)
                    .appliedAmount(new BigDecimal("10000.00"))
                    .approvedAmount(new BigDecimal("10000.00"))
                    .purpose("Business Expansion")
                    .build();
            loanApplicationRepository.save(activeLoan1);
            
            LoanApplication activeLoan2 = LoanApplication.builder()
                    .applicationNumber("APP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .applicant(dummyUser)
                    .status(ApplicationStatus.APPROVED)
                    .appliedAmount(new BigDecimal("5000.00"))
                    .approvedAmount(new BigDecimal("5000.00"))
                    .purpose("Equipment Purchase")
                    .build();
            loanApplicationRepository.save(activeLoan2);

            // Create some dummy pending applications
            LoanApplication pendingLoan1 = LoanApplication.builder()
                    .applicationNumber("APP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .applicant(dummyUser)
                    .status(ApplicationStatus.SUBMITTED)
                    .appliedAmount(new BigDecimal("2000.00"))
                    .purpose("Working Capital")
                    .build();
            loanApplicationRepository.save(pendingLoan1);
            
            log.info("Dummy test user created successfully with active and pending loans.");
        }
    }
}
