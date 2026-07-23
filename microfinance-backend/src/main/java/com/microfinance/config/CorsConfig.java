package com.microfinance.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * CORS configuration for the Microfinance Platform backend.
 *
 * <p>Allows the Angular frontend (localhost:4200) to communicate with the
 * Spring Boot REST APIs during development. This configuration will be
 * extended with environment-specific origins in production.
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Allow Angular dev server origin
        configuration.setAllowedOrigins(List.of("http://localhost:4200"));

        // Allow all standard HTTP methods used by REST APIs
        configuration.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));

        // Allow all headers (Authorization for JWT will be added in Security US)
        configuration.setAllowedHeaders(List.of("*"));

        // Allow cookies and credentials (required for JWT bearer tokens)
        configuration.setAllowCredentials(true);

        // Cache preflight response for 1 hour (reduces OPTIONS requests)
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
