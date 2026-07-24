package com.microfinance.service;

import com.microfinance.dto.StaffCreationRequestDTO;
import com.microfinance.dto.StaffDTO;
import com.microfinance.dto.StaffPasswordResponseDTO;
import com.microfinance.entity.AuditLog;
import com.microfinance.entity.User;
import com.microfinance.enums.UserRole;
import com.microfinance.repository.AuditLogRepository;
import com.microfinance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminStaffService {

    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%";
    private static final SecureRandom RANDOM = new SecureRandom();

    @Transactional(readOnly = true)
    public List<StaffDTO> getAllStaff() {
        return userRepository.findByRoleIn(List.of(UserRole.ROLE_OFFICER, UserRole.ROLE_ADMIN)).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public StaffPasswordResponseDTO createStaff(StaffCreationRequestDTO request, String adminUsername) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already registered");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username is already taken");
        }

        String tempPassword = generateRandomPassword();

        User user = User.builder()
                .email(request.getEmail())
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(tempPassword))
                .role(request.getRole() != null ? request.getRole() : UserRole.ROLE_OFFICER)
                .active(true)
                .mustChangePassword(true)
                .build();

        userRepository.save(user);

        logAction("STAFF_CREATED", "Admin " + adminUsername + " created staff account for " + request.getEmail(), user.getId());

        return new StaffPasswordResponseDTO(tempPassword);
    }

    @Transactional
    public void disableStaff(Long id, String adminUsername) {
        User user = getUserById(id);
        user.setActive(false);
        userRepository.save(user);

        logAction("STAFF_DISABLED", "Admin " + adminUsername + " disabled staff account for " + user.getEmail(), user.getId());
    }

    @Transactional
    public void enableStaff(Long id, String adminUsername) {
        User user = getUserById(id);
        user.setActive(true);
        userRepository.save(user);

        logAction("STAFF_ENABLED", "Admin " + adminUsername + " enabled staff account for " + user.getEmail(), user.getId());
    }

    @Transactional
    public StaffPasswordResponseDTO resetStaffPassword(Long id, String adminUsername) {
        User user = getUserById(id);
        
        String newTempPassword = generateRandomPassword();
        user.setPasswordHash(passwordEncoder.encode(newTempPassword));
        user.setMustChangePassword(true);
        
        userRepository.save(user);

        logAction("STAFF_PASSWORD_RESET", "Admin " + adminUsername + " reset password for staff account " + user.getEmail(), user.getId());

        return new StaffPasswordResponseDTO(newTempPassword);
    }

    private User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private void logAction(String action, String description, Long entityId) {
        AuditLog auditLog = AuditLog.builder()
                .entityType("USER")
                .entityId(entityId)
                .action(action)
                .newValue(description) // Storing description in newValue for logging purpose
                .build();
        auditLogRepository.save(auditLog);
    }

    private String generateRandomPassword() {
        StringBuilder sb = new StringBuilder(10);
        for (int i = 0; i < 10; i++) {
            sb.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }

    private StaffDTO mapToDTO(User user) {
        return StaffDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .role(user.getRole())
                .active(user.isActive())
                .build();
    }
}
