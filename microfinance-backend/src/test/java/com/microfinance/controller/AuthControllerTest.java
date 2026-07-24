package com.microfinance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microfinance.dto.LoginRequest;
import com.microfinance.entity.User;
import com.microfinance.enums.UserRole;
import com.microfinance.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        User user = User.builder()
                .username("testapplicant")
                .email("applicant@test.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .role(UserRole.ROLE_APPLICANT)
                .active(true)
                .build();
        userRepository.save(user);

        User officer = User.builder()
            .username("testofficer")
            .email("officer@test.com")
            .passwordHash(passwordEncoder.encode("officer123"))
            .role(UserRole.ROLE_OFFICER)
            .active(true)
            .build();
        userRepository.save(officer);
    }

    @Test
    void shouldAuthenticateValidUserAndReturnToken() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("applicant@test.com");
        loginRequest.setPassword("password123");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.email").value("applicant@test.com"))
                .andExpect(jsonPath("$.role").value("ROLE_APPLICANT"));
    }

    @Test
    void shouldReturnUnauthorizedForWrongPassword() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("applicant@test.com");
        loginRequest.setPassword("wrongpassword");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAuthenticateOfficerUsingUsername() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("testofficer");
        loginRequest.setPassword("officer123");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("officer@test.com"))
                .andExpect(jsonPath("$.role").value("ROLE_OFFICER"));
    }

    @Test
    void shouldReturnUnauthorizedForUnknownEmail() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("unknown@test.com");
        loginRequest.setPassword("password123");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }
}
