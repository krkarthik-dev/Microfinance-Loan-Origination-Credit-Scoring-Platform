package com.microfinance.service;

import com.microfinance.dto.*;
import com.microfinance.entity.AuditLog;
import com.microfinance.entity.PasswordResetRequest;
import com.microfinance.entity.User;
import com.microfinance.entity.UserProfile;
import com.microfinance.repository.AuditLogRepository;
import com.microfinance.repository.PasswordResetRequestRepository;
import com.microfinance.repository.UserProfileRepository;
import com.microfinance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private final PasswordResetRequestRepository passwordResetRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogRepository auditLogRepository;

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Triggered when user clicks Forgot Password and enters their email.
     * Generates a request ID RT-XXXXXX (x - 6 digit number) and notifies user.
     */
    @Transactional
    public ForgotPasswordResponseDTO createResetRequest(ForgotPasswordRequestDTO request) {
        log.info("Received forgot password request for email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .or(() -> userRepository.findByUsername(request.getEmail()))
                .orElseThrow(() -> new IllegalArgumentException("No account found with email address: " + request.getEmail()));

        String firstName = user.getUsername();
        String lastName = "";

        var profileOpt = userProfileRepository.findByUserId(user.getId());
        if (profileOpt.isPresent()) {
            UserProfile profile = profileOpt.get();
            if (profile.getFirstName() != null && !profile.getFirstName().isBlank()) {
                firstName = profile.getFirstName();
            }
            if (profile.getLastName() != null) {
                lastName = profile.getLastName();
            }
        }

        // Generate unique RT-XXXXXX request id
        String requestId;
        do {
            int num = 100000 + RANDOM.nextInt(900000); // 6-digit number
            requestId = "RT-" + num;
        } while (passwordResetRepository.existsByRequestId(requestId));

        PasswordResetRequest resetRequest = PasswordResetRequest.builder()
                .requestId(requestId)
                .user(user)
                .email(user.getEmail())
                .firstName(firstName)
                .lastName(lastName)
                .status("PENDING")
                .build();

        passwordResetRepository.save(resetRequest);

        log.info("Created password reset request {} for user {}", requestId, user.getEmail());

        String message = "contact bank with this request id " + requestId + " for temp password";
        return ForgotPasswordResponseDTO.builder()
                .requestId(requestId)
                .message(message)
                .build();
    }

    /**
     * Officer Command Center: Get all pending password reset requests for grid display.
     */
    @Transactional(readOnly = true)
    public List<PasswordResetSummaryDTO> getPendingRequests() {
        return passwordResetRepository.findByStatusOrderByCreatedAtDesc("PENDING").stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Officer Command Center: Get all password reset requests (pending & approved).
     */
    @Transactional(readOnly = true)
    public List<PasswordResetSummaryDTO> getAllRequests() {
        return passwordResetRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Officer approves the password reset request in the grid.
     * Generates a temporary password, hashes it, and forces the user to change password on first login.
     */
    @Transactional
    public PasswordResetApprovalResponseDTO approveRequest(String requestId, String officerUsername) {
        log.info("Officer {} approving password reset request {}", officerUsername, requestId);

        PasswordResetRequest resetRequest = passwordResetRepository.findByRequestId(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Password reset request not found: " + requestId));

        if (!"PENDING".equalsIgnoreCase(resetRequest.getStatus())) {
            throw new IllegalStateException("Request is already " + resetRequest.getStatus() + ". Only PENDING requests can be approved.");
        }

        User user = resetRequest.getUser();
        if (user == null) {
            user = userRepository.findByEmail(resetRequest.getEmail())
                    .orElseThrow(() -> new IllegalArgumentException("User no longer exists for email: " + resetRequest.getEmail()));
        }

        // Generate temporary password (e.g. TempPass@8492) matching strength regex
        int randomDigits = 1000 + RANDOM.nextInt(9000);
        String tempPassword = "TempPass@" + randomDigits;

        user.setPasswordHash(passwordEncoder.encode(tempPassword));
        user.setMustChangePassword(true); // Triggers forced password change on first login!
        userRepository.save(user);

        resetRequest.setStatus("APPROVED");
        resetRequest.setTempPassword(tempPassword);
        resetRequest.setResolvedAt(LocalDateTime.now());
        passwordResetRepository.save(resetRequest);

        AuditLog auditLog = AuditLog.builder()
                .entityType("USER")
                .entityId(user.getId())
                .action("PASSWORD_RESET_APPROVED")
                .newValue("Officer " + officerUsername + " approved request " + requestId + ". Issued temp password.")
                .build();
        auditLogRepository.save(auditLog);

        log.info("Approved reset request {} for user {}. Temporary password issued.", requestId, user.getEmail());

        return PasswordResetApprovalResponseDTO.builder()
                .requestId(requestId)
                .email(user.getEmail())
                .tempPassword(tempPassword)
                .message("Password reset approved. Temporary password issued: " + tempPassword + ". User will be forced to change it on login.")
                .build();
    }

    private PasswordResetSummaryDTO mapToDTO(PasswordResetRequest request) {
        return PasswordResetSummaryDTO.builder()
                .id(request.getId())
                .requestId(request.getRequestId())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .status(request.getStatus())
                .tempPassword(request.getTempPassword())
                .createdAt(request.getCreatedAt())
                .resolvedAt(request.getResolvedAt())
                .build();
    }
}
