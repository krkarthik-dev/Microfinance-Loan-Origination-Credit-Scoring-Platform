package com.microfinance.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import com.microfinance.security.JwtAuthenticationEntryPoint;
import com.microfinance.security.JwtAuthenticationFilter;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * Unit tests for {@link HealthController}.
 *
 * <p>Uses @WebMvcTest to load only the web layer (no full Spring context),
 * and disables security filters to isolate controller behavior.
 * Security integration tests (verifying /api/health is publicly accessible)
 * will be covered in the dedicated Security User Story.
 */
@WebMvcTest(HealthController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("HealthController Unit Tests")
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @DisplayName("GET /api/health should return HTTP 200 OK")
    void shouldReturn200Ok() throws Exception {
        mockMvc.perform(get("/api/health")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/health should return status UP")
    void shouldReturnStatusUp() throws Exception {
        mockMvc.perform(get("/api/health")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("GET /api/health should return correct service name")
    void shouldReturnCorrectServiceName() throws Exception {
        mockMvc.perform(get("/api/health")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("Microfinance Loan Origination Platform"));
    }

    @Test
    @DisplayName("GET /api/health should return version 1.0.0")
    void shouldReturnVersion() throws Exception {
        mockMvc.perform(get("/api/health")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value("1.0.0"));
    }

    @Test
    @DisplayName("GET /api/health should return a non-null timestamp")
    void shouldReturnTimestamp() throws Exception {
        mockMvc.perform(get("/api/health")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    @DisplayName("GET /api/health should return application/json content type")
    void shouldReturnJsonContentType() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("GET /api/health response should contain all expected fields")
    void shouldReturnAllExpectedFields() throws Exception {
        mockMvc.perform(get("/api/health")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", notNullValue()))
                .andExpect(jsonPath("$.service", notNullValue()))
                .andExpect(jsonPath("$.version", notNullValue()))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }
}
