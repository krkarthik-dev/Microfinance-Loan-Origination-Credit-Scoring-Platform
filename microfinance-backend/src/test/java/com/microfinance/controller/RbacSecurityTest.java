package com.microfinance.controller;

import com.microfinance.entity.User;
import com.microfinance.enums.UserRole;
import com.microfinance.repository.UserRepository;
import com.microfinance.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AC3: Verifies Spring Security filters reject requests if correct role claim is missing.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RbacSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private UserRepository userRepository;

    private String applicantToken;
    private String officerToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        applicantToken = createTokenForRole("applicant@test.com", UserRole.ROLE_APPLICANT);
        officerToken = createTokenForRole("officer@test.com", UserRole.ROLE_OFFICER);
        adminToken = createTokenForRole("admin@test.com", UserRole.ROLE_ADMIN);
    }

    private String createTokenForRole(String email, UserRole role) {
        User user = User.builder()
                .username(email.split("@")[0])
                .email(email)
                .passwordHash("hashed")
                .role(role)
                .active(true)
                .build();
        userRepository.save(user);

        org.springframework.security.core.userdetails.User principal = 
                new org.springframework.security.core.userdetails.User(
                        email, "password", 
                        Collections.singletonList(new SimpleGrantedAuthority(role.name()))
                );

        return tokenProvider.generateToken(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    @Test
    void shouldDenyUnauthenticatedAccess() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowAdminAccessToAdminDashboard() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void shouldDenyApplicantAccessToAdminDashboard() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard")
                .header("Authorization", "Bearer " + applicantToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowOfficerAccessToOfficerDashboard() throws Exception {
        mockMvc.perform(get("/api/officer/dashboard")
                .header("Authorization", "Bearer " + officerToken))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowAdminAccessToOfficerDashboard() throws Exception {
        mockMvc.perform(get("/api/officer/dashboard")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void shouldDenyApplicantAccessToOfficerDashboard() throws Exception {
        mockMvc.perform(get("/api/officer/dashboard")
                .header("Authorization", "Bearer " + applicantToken))
                .andExpect(status().isForbidden());
    }
    
    @Test
    void shouldAllowApplicantAccessToApplicantDashboard() throws Exception {
        mockMvc.perform(get("/api/applicant/dashboard")
                .header("Authorization", "Bearer " + applicantToken))
                .andExpect(status().isOk());
    }
}
