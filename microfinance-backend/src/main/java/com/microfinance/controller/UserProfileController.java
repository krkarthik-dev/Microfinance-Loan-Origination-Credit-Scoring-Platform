package com.microfinance.controller;

import com.microfinance.dto.UserProfileDto;
import com.microfinance.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/applicant/profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    @GetMapping
    @PreAuthorize("hasRole('APPLICANT')")
    public ResponseEntity<UserProfileDto> getProfile(Authentication authentication) {
        UserProfileDto profile = userProfileService.getProfile(authentication.getName());
        return ResponseEntity.ok(profile);
    }

    @PutMapping
    @PreAuthorize("hasRole('APPLICANT')")
    public ResponseEntity<UserProfileDto> updateProfile(
            @Valid @RequestBody UserProfileDto dto,
            Authentication authentication) {
        
        UserProfileDto updatedProfile = userProfileService.updateProfile(authentication.getName(), dto);
        return ResponseEntity.ok(updatedProfile);
    }
}
