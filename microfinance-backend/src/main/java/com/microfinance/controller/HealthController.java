package com.microfinance.controller;

import com.microfinance.dto.HealthResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * REST controller exposing a health check endpoint.
 *
 * <p>This endpoint is publicly accessible (no JWT required) and is used to
 * verify end-to-end connectivity between the Angular frontend and the
 * Spring Boot backend (AC4 of US01).
 *
 * <p>Endpoint: GET /api/health
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    /**
     * Returns the current health status of the Microfinance Platform backend.
     *
     * @return HTTP 200 with a {@link HealthResponse} payload
     */
    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        HealthResponse response = HealthResponse.builder()
                .status("UP")
                .service("Microfinance Loan Origination Platform")
                .version("1.0.0")
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.ok(response);
    }
}
