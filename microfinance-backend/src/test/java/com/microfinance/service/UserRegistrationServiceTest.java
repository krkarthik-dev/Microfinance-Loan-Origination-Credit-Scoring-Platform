package com.microfinance.service;

import com.microfinance.entity.User;
import com.microfinance.entity.UserProfile;
import com.microfinance.enums.UserRole;
import com.microfinance.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for {@link UserRegistrationService}.
 *
 * <p>Uses @SpringBootTest with the "test" profile (H2 in-memory, Flyway disabled).
 * Tests are NOT annotated with @Transactional at the test level — this is intentional
 * so that Spring's transaction boundaries behave exactly as they would in production.
 *
 * <p><b>AC3 — ACID Rollback Verification:</b><br>
 * {@code shouldRollbackUserSave_WhenProfileIsNull_ACID} proves that if the profile
 * save fails after the user has already been flushed to H2, the entire transaction
 * is rolled back — the user is NOT persisted in the database.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("UserRegistrationService — ACID & Integration Tests")
class UserRegistrationServiceTest {

    @Autowired
    private UserRegistrationService userRegistrationService;

    @Autowired
    private UserRepository userRepository;

    // ─── Test Data Builders ────────────────────────────────────────────────────

    private User buildUser(String email, String username) {
        return User.builder()
                .email(email)
                .username(username)
                .passwordHash("$2a$10$hashedpassword")
                .role(UserRole.ROLE_APPLICANT)
                .active(true)
                .build();
    }

    private UserProfile buildProfile(String phone) {
        return UserProfile.builder()
                .firstName("Ravi")
                .lastName("Kumar")
                .dateOfBirth(LocalDate.of(1990, 5, 15))
                .gender("MALE")
                .phoneNumber(phone)
                .addressLine1("12, MG Road")
                .city("Bengaluru")
                .state("Karnataka")
                .pincode("560001")
                .build();
    }

    // ─── Test Cases ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("AC3: Should persist both User and UserProfile when registration succeeds")
    void shouldRegisterUserWithProfile_WhenValidDataProvided() {
        // Arrange
        User user = buildUser("ravi.kumar@test.com", "ravi_kumar");
        UserProfile profile = buildProfile("9876543210");

        // Act
        User result = userRegistrationService.registerUserWithProfile(user, profile);

        // Assert — user saved
        assertThat(result).isNotNull();
        assertThat(result.getId()).isNotNull().isPositive();
        assertThat(userRepository.findByEmail("ravi.kumar@test.com")).isPresent();
    }

    @Test
    @DisplayName("AC3: Should rollback User save when profile is null (ACID compliance)")
    void shouldRollbackUserSave_WhenProfileIsNull_ACID() {
        // Arrange — unique email to avoid conflicts with other tests
        User user = buildUser("rollback.test@test.com", "rollback_user");

        // Act — pass null profile to trigger exception AFTER user saveAndFlush
        assertThatThrownBy(() ->
                userRegistrationService.registerUserWithProfile(user, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("UserProfile cannot be null");

        // Assert — ACID: user must NOT be in the database (transaction rolled back)
        Optional<User> savedUser = userRepository.findByEmail("rollback.test@test.com");
        assertThat(savedUser)
                .as("User should NOT be persisted because the transaction was rolled back")
                .isEmpty();
    }

    @Test
    @DisplayName("Should find user by email when user exists")
    void shouldFindUserByEmail_WhenUserExists() {
        // Arrange
        User user = buildUser("find.me@test.com", "find_me_user");
        UserProfile profile = buildProfile("9123456780");
        userRegistrationService.registerUserWithProfile(user, profile);

        // Act
        Optional<User> result = userRegistrationService.findByEmail("find.me@test.com");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("find.me@test.com");
        assertThat(result.get().getRole()).isEqualTo(UserRole.ROLE_APPLICANT);
    }

    @Test
    @DisplayName("Should return empty Optional when email is not registered")
    void shouldReturnEmpty_WhenEmailNotFound() {
        // Act
        Optional<User> result = userRegistrationService.findByEmail("nonexistent@test.com");

        // Assert
        assertThat(result).isEmpty();
    }
}
