package com.microfinance.controller;

import com.microfinance.dto.PasswordResetApprovalResponseDTO;
import com.microfinance.dto.PasswordResetSummaryDTO;
import com.microfinance.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/officer/password-resets")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('OFFICER', 'ADMIN')")
public class OfficerPasswordResetController {

    private final PasswordResetService passwordResetService;

    @GetMapping
    public ResponseEntity<List<PasswordResetSummaryDTO>> getAllRequests() {
        return ResponseEntity.ok(passwordResetService.getAllRequests());
    }

    @GetMapping("/pending")
    public ResponseEntity<List<PasswordResetSummaryDTO>> getPendingRequests() {
        return ResponseEntity.ok(passwordResetService.getPendingRequests());
    }

    @PostMapping("/{requestId}/approve")
    public ResponseEntity<PasswordResetApprovalResponseDTO> approveRequest(
            @PathVariable("requestId") String requestId,
            Authentication authentication) {
        String officerUsername = authentication != null ? authentication.getName() : "system_officer";
        PasswordResetApprovalResponseDTO response = passwordResetService.approveRequest(requestId, officerUsername);
        return ResponseEntity.ok(response);
    }
}
