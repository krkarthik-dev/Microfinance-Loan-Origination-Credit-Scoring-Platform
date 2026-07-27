package com.microfinance.config;

import com.microfinance.entity.User;
import com.microfinance.entity.UserProfile;
import com.microfinance.enums.UserRole;
import com.microfinance.repository.UserProfileRepository;
import com.microfinance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * US65 (Revised): One-Time Forceful Database Cleanup & Baseline Seeding
 * Automated startup script that initializes dev/test databases exactly once per lifecycle.
 * Leaves a permanent marker in seeding_history table to preserve subsequent test data on restarts.
 */
@Component
@Profile({"dev", "test", "local", "default"})
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class DevBaselineSeeder implements CommandLineRunner {

    public static final String SCRIPT_NAME = "DEV_BASELINE_SEED_V1";

    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;
    private final TransactionTemplate transactionTemplate;

    @Override
    public void run(String... args) throws Exception {
        verifyAllowedEnvironment();

        if (isAlreadyExecuted()) {
            log.info("US65: Dev baseline seeding ('{}') has already been executed for this database lifecycle. Bypassing cleanup and seeding to preserve existing data.", SCRIPT_NAME);
            return;
        }

        log.info("US65: Initial boot detected. Starting one-time forceful database cleanup and baseline seeding...");

        transactionTemplate.executeWithoutResult(status -> {
            try {
                // 1. Transactional Data Purge
                purgeTransactionalTables();

                // 2. Admin Bootstrapping
                seedAdminAccount();

                // 3. Loan Officer Seeding
                seedLoanOfficers();

                // 4. Borrower Profile Seeding
                seedBorrowers();

                // 5. Register permanent execution marker
                recordExecutionMarker();

                log.info("US65: One-time database cleanup and baseline seeding completed successfully.");
            } catch (Exception e) {
                status.setRollbackOnly();
                log.error("US65: Failed during baseline seeding execution: ", e);
                throw new RuntimeException("Database baseline seeding failed", e);
            }
        });
    }

    private boolean isAlreadyExecuted() {
        try {
            // Ensure table exists in case Flyway hasn't initialized it in certain test contexts
            jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS seeding_history (" +
                "id SERIAL PRIMARY KEY, " +
                "script_name VARCHAR(100) NOT NULL UNIQUE, " +
                "executed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "status VARCHAR(20) NOT NULL)"
            );
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM seeding_history WHERE script_name = ?",
                Integer.class, SCRIPT_NAME
            );
            return count != null && count > 0;
        } catch (Exception e) {
            log.warn("Could not query seeding_history table: {}. Assuming not executed.", e.getMessage());
            return false;
        }
    }

    private void recordExecutionMarker() {
        jdbcTemplate.update(
            "INSERT INTO seeding_history (script_name, status, executed_at) VALUES (?, ?, CURRENT_TIMESTAMP)",
            SCRIPT_NAME, "COMPLETED"
        );
        log.info("US65: Registered permanent execution marker '{}' in seeding_history table.", SCRIPT_NAME);
    }

    private void verifyAllowedEnvironment() {
        String[] activeProfiles = environment.getActiveProfiles();
        for (String profile : activeProfiles) {
            if ("prod".equalsIgnoreCase(profile) || "production".equalsIgnoreCase(profile)) {
                log.error("CRITICAL SECURITY VIOLATION: Attempted dev baseline seeding in PRODUCTION profile!");
                throw new SecurityException("Database seeding script is strictly forbidden in production environments.");
            }
        }
    }

    private void purgeTransactionalTables() {
        log.info("Purging transactional and operational tables...");
        try {
            jdbcTemplate.execute(
                "TRUNCATE TABLE loan_applications, emi_installments, audit_logs, kyc_documents, " +
                "system_notifications, loan_correction_requests, loan_documents, credit_scores, " +
                "loan_decisions, disbursement_queue, user_profiles, users, loan_id_sequence " +
                "RESTART IDENTITY CASCADE"
            );
        } catch (Exception e) {
            log.warn("PostgreSQL TRUNCATE CASCADE failed (likely test environment). Using fallback RI-disabled truncate.");
            jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
            String[] tables = {
                "loan_applications", "emi_installments", "audit_logs", "kyc_documents",
                "system_notifications", "loan_correction_requests", "loan_documents",
                "credit_scores", "loan_decisions", "disbursement_queue", "user_profiles",
                "users", "loan_id_sequence"
            };
            for (String table : tables) {
                try {
                    jdbcTemplate.execute("TRUNCATE TABLE " + table);
                } catch (Exception ignored) {}
            }
            jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
        }
    }

    private void seedAdminAccount() {
        User admin = User.builder()
                .email("admin@microfinance.com")
                .username("admin")
                .passwordHash(passwordEncoder.encode("Admin@123"))
                .role(UserRole.ROLE_ADMIN)
                .active(true)
                .mustChangePassword(false)
                .build();
        userRepository.save(admin);
        log.info("Seeded Admin: admin@microfinance.com");
    }

    private void seedLoanOfficers() {
        String[] firstNames = {"Rajesh", "Anjali", "Vikram", "Priya", "Amitabh"};
        String[] lastNames = {"Sharma", "Verma", "Singh", "Patel", "Gupta"};
        String[] branches = {"BR-MUMBAI-01", "BR-DELHI-02", "BR-BLR-03", "BR-CHN-04", "BR-KOL-05"};
        String[] cities = {"Mumbai", "New Delhi", "Bengaluru", "Chennai", "Kolkata"};

        for (int i = 1; i <= 5; i++) {
            User officer = User.builder()
                    .email("officer" + i + "@microfinance.com")
                    .username("officer" + i)
                    .passwordHash(passwordEncoder.encode("Officer@123"))
                    .role(UserRole.ROLE_OFFICER)
                    .active(true)
                    .mustChangePassword(false)
                    .build();
            userRepository.save(officer);

            UserProfile profile = UserProfile.builder()
                    .user(officer)
                    .firstName(firstNames[i - 1])
                    .lastName(lastNames[i - 1])
                    .dateOfBirth(LocalDate.of(1982 + i, 5, 10))
                    .gender(i % 2 == 0 ? "FEMALE" : "MALE")
                    .phoneNumber("987650000" + i)
                    .addressLine1("Branch Office " + branches[i - 1])
                    .city(cities[i - 1])
                    .state("Maharashtra")
                    .pincode("40000" + i)
                    .kycVerified(true)
                    .build();
            userProfileRepository.save(profile);
            log.info("Seeded Officer: officer{}@microfinance.com ({})", i, branches[i - 1]);
        }
    }

    private void seedBorrowers() {
        String[] firstNames = {"Rahul", "Sneha", "Amit", "Pooja", "Arjun"};
        String[] lastNames = {"Nair", "Desai", "Chowdhury", "Mehta", "Kulkarni"};
        String[] cities = {"Mumbai", "New Delhi", "Bengaluru", "Chennai", "Pune"};
        String[] states = {"Maharashtra", "Delhi", "Karnataka", "Tamil Nadu", "Maharashtra"};
        String[] employments = {"Salaried", "Salaried", "Self-Employed", "Salaried", "Professional"};
        BigDecimal[] incomes = {
            new BigDecimal("85000"), new BigDecimal("120000"),
            new BigDecimal("65000"), new BigDecimal("95000"),
            new BigDecimal("150000")
        };

        for (int i = 1; i <= 5; i++) {
            User borrower = User.builder()
                    .email("borrower" + i + "@microfinance.com")
                    .username("borrower" + i)
                    .passwordHash(passwordEncoder.encode("Borrower@123"))
                    .role(UserRole.ROLE_APPLICANT)
                    .active(true)
                    .mustChangePassword(false)
                    .build();
            userRepository.save(borrower);

            UserProfile profile = UserProfile.builder()
                    .user(borrower)
                    .firstName(firstNames[i - 1])
                    .lastName(lastNames[i - 1])
                    .dateOfBirth(LocalDate.of(1990 + i, 3, 15))
                    .gender(i % 2 == 0 ? "FEMALE" : "MALE")
                    .phoneNumber("998870000" + i)
                    .addressLine1("123, Residency Road, Sector " + i)
                    .city(cities[i - 1])
                    .state(states[i - 1])
                    .pincode("50000" + i)
                    .panNumber(String.format("ABCDE100%dF", i))
                    .aadhaarNumber(String.format("99998888000%d", i))
                    .employmentType(employments[i - 1])
                    .monthlyIncome(incomes[i - 1])
                    .kycVerified(true)
                    .build();
            userProfileRepository.save(profile);
            log.info("Seeded Borrower: borrower{}@microfinance.com ({})", i, cities[i - 1]);
        }
    }
}
