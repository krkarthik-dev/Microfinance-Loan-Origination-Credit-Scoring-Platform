package com.microfinance.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for the /api/health endpoint.
 *
 * <p>Returns platform status, service name, version, and server timestamp
 * to confirm that the backend is running and reachable from the frontend.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthResponse {

    /** Current health status of the application (e.g., "UP"). */
    private String status;

    /** Human-readable name of the service. */
    private String service;

    /** Current application version. */
    private String version;

    /** Server timestamp at the time of the health check request. */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
}
