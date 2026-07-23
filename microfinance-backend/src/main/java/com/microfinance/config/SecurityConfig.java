package com.microfinance.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Base Security configuration for the Microfinance Platform.
 *
 * <p>This is a foundational setup for US01. Full JWT authentication,
 * role-based access control, and endpoint-level authorization will be
 * implemented in a dedicated Security User Story.
 *
 * <p>For US01, only /api/health is explicitly permitted without authentication
 * to validate end-to-end connectivity between Angular and Spring Boot.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CorsConfigurationSource corsConfigurationSource;

    public SecurityConfig(CorsConfigurationSource corsConfigurationSource) {
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Apply CORS configuration (allows Angular dev server requests)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))

                // Disable CSRF — stateless REST API uses JWT tokens, not sessions
                .csrf(AbstractHttpConfigurer::disable)

                // Stateless session — no server-side session storage
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Endpoint authorization rules
                .authorizeHttpRequests(auth -> auth
                        // Health check is publicly accessible (no token required)
                        .requestMatchers("/api/health").permitAll()
                        // All other endpoints require authentication (JWT to be added)
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}
