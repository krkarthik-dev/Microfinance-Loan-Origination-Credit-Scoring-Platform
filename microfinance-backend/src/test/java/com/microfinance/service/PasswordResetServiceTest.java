package com.microfinance.service;

import com.microfinance.dto.ForgotPasswordRequestDTO;
import com.microfinance.dto.ForgotPasswordResponseDTO;
import com.microfinance.dto.PasswordResetApprovalResponseDTO;
import com.microfinance.dto.PasswordResetSummaryDTO;
import com.microfinance.entity.User;
import com.microfinance.entity.UserProfile;
import com.microfinance.enums.UserRole;
import com.microfinance.repository.PasswordResetRequestRepository;
import com.microfinance.repository.UserProfileRepository;
import com.microfinance.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("PasswordResetService — Integration Tests")
class PasswordResetServiceTest {

    @Autowired
    private PasswordResetService passwordResetService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private PasswordResetRequestRepository passwordResetRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;

    @BeforeEach
    void setUp() {
        passwordResetRepository.deleteAll();
        userProfileRepository.deleteAll();
        userRepository.deleteAll();

        testUser = User.builder()
                .email("borrower.reset@test.com")
                .username("borrower_reset")
                .passwordHash(passwordEncoder.encode("OldPassword123!"))
                .role(UserRole.ROLE_APPLICANT)
                .active(true)
                .mustChangePassword(false)
                .build();
        testUser = userRepository.save(testUser);

        UserProfile profile = UserProfile.builder()
                .user(testUser)
                .firstName("John")
                .lastName("Doe")
                .build();
        userProfileRepository.save(profile);
    }

    @Test
    @DisplayName("Should create RT-XXXXXX request and return message")
    void shouldCreateResetRequest() {
        ForgotPasswordRequestDTO request = new ForgotPasswordRequestDTO("borrower.reset@test.com");
        ForgotPasswordResponseDTO response = passwordResetService.createResetRequest(request);

        assertThat(response).isNotNull();
        assertThat(response.getRequestId()).startsWith("RT-").hasSize(9);
        assertThat(response.getMessage()).contains("contact bank with this request id").contains(response.getRequestId());

        List<PasswordResetSummaryDTO> pending = passwordResetService.getPendingRequests();
        assertThat(pending).hasSize(1);
        assertThat(pending.get(0).getFirstName()).isEqualTo("John");
        assertThat(pending.get(0).getLastName()).isEqualTo("Doe");
        assertThat(pending.get(0).getStatus()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("Should approve reset request, generate temp pass, and force password change")
    void shouldApproveRequestAndForcePasswordChange() {
        ForgotPasswordRequestDTO request = new ForgotPasswordRequestDTO("borrower.reset@test.com");
        ForgotPasswordResponseDTO response = passwordResetService.createResetRequest(request);

        PasswordResetApprovalResponseDTO approval = passwordResetService.approveRequest(response.getRequestId(), "officer1");

        assertThat(approval.getTempPassword()).startsWith("TempPass@");
        assertThat(approval.getMessage()).contains("Temporary password issued");

        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(updatedUser.isMustChangePassword()).isTrue();
        assertThat(passwordEncoder.matches(approval.getTempPassword(), updatedUser.getPasswordHash())).isTrue();

        List<PasswordResetSummaryDTO> pending = passwordResetService.getPendingRequests();
        assertThat(pending).isEmpty();
    }
}
