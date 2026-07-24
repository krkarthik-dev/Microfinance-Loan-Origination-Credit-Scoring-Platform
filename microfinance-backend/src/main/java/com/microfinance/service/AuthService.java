package com.microfinance.service;

import com.microfinance.dto.ChangePasswordRequestDTO;
import com.microfinance.dto.LoginRequest;
import com.microfinance.dto.LoginResponse;
import com.microfinance.entity.User;
import com.microfinance.entity.UserProfile;
import com.microfinance.enums.UserRole;
import com.microfinance.repository.UserProfileRepository;
import com.microfinance.repository.UserRepository;
import com.microfinance.security.JwtTokenProvider;
import com.microfinance.dto.SignupRequestDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;

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

        User user = userRepository.findByEmail(loginRequest.getEmail())
                .or(() -> userRepository.findByUsername(loginRequest.getEmail()))
                .orElseThrow(() -> new RuntimeException("User not found"));

        log.info("User {} authenticated successfully. Issued JWT with role: {}, mustChange: {}", 
                 loginRequest.getEmail(), role, user.isMustChangePassword());

        return LoginResponse.builder()
                .token(jwt)
                .email(user.getEmail())
                .role(role)
                .mustChangePassword(user.isMustChangePassword())
                .build();
    }

    /**
     * AC4: Registers a new borrower and returns a JWT instantly.
     */
    public LoginResponse register(SignupRequestDTO request) {
        log.info("Registering new user with email: {}", request.getEmail());
        
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already in use");
        }

        User user = User.builder()
                .username(request.getEmail())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.ROLE_APPLICANT)
                .active(true)
                .mustChangePassword(false)
                .build();

        User savedUser = userRepository.save(user);

        UserProfile profile = UserProfile.builder()
                .user(savedUser)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .build();

        userProfileRepository.save(profile);

        // Authenticate the newly created user to generate JWT
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(request.getEmail());
        loginRequest.setPassword(request.getPassword());
        
        return login(loginRequest);
    }

    /**
     * Changes the user's password and removes the mustChangePassword flag.
     */
    public void changePassword(ChangePasswordRequestDTO request) {
        // Authenticate the old password
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getOldPassword()
                )
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setMustChangePassword(false);
        userRepository.save(user);

        log.info("User {} successfully changed their mandatory temporary password.", user.getEmail());
    }
}
