package com.microfinance.controller;

import com.microfinance.dto.ChangePasswordRequestDTO;
import com.microfinance.dto.LoginRequest;
import com.microfinance.dto.LoginResponse;
import com.microfinance.dto.SignupRequestDTO;
import com.microfinance.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for public authentication endpoints.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * AC1: /api/auth/login endpoint issuing a JWT.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        LoginResponse jwtResponse = authService.login(loginRequest);
        return ResponseEntity.ok(jwtResponse);
    }

    /**
     * AC4: Registers a new borrower and returns a JWT.
     */
    @PostMapping("/signup")
    public ResponseEntity<LoginResponse> registerUser(@Valid @RequestBody SignupRequestDTO signupRequest) {
        LoginResponse jwtResponse = authService.register(signupRequest);
        return ResponseEntity.ok(jwtResponse);
    }

    /**
     * US22: Mandatory password change for temp passwords.
     */
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequestDTO request) {
        authService.changePassword(request);
        return ResponseEntity.ok().build();
    }
}
