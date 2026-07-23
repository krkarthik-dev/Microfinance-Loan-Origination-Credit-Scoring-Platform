package com.microfinance.service;

import com.microfinance.dto.LoginRequest;
import com.microfinance.dto.LoginResponse;
import com.microfinance.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    /**
     * Authenticates the user and returns a JWT token.
     *
     * @param loginRequest the credentials
     * @return LoginResponse containing the JWT
     */
    public LoginResponse login(LoginRequest loginRequest) {
        log.debug("Attempting authentication for user: {}", loginRequest.getEmail());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = tokenProvider.generateToken(authentication);

        // Extract role for the response DTO
        String role = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("ROLE_APPLICANT");

        log.info("User {} authenticated successfully. Issued JWT with role: {}", loginRequest.getEmail(), role);

        return LoginResponse.builder()
                .token(jwt)
                .email(loginRequest.getEmail())
                .role(role)
                .build();
    }
}
