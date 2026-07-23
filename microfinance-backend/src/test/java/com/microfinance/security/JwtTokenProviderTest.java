package com.microfinance.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        // Valid Base64 encoded key (256-bit)
        String secret = "M2FjNzhlZjkyZDE0YzRlOWI4YTNjZGFjYzYwNjVhOGYzYmQ5MjU3NzI0OWMwNDRlYzc2NWRjMjQ0ZjJiNzNkNQ==";
        long expiration = 86400000; // 24 hours
        tokenProvider = new JwtTokenProvider(secret, expiration);

        User principal = new User("test@example.com", "password", 
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_APPLICANT")));
        authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    @Test
    void shouldGenerateValidToken() {
        String token = tokenProvider.generateToken(authentication);
        
        assertThat(token).isNotNull().isNotEmpty();
        assertThat(tokenProvider.validateToken(token)).isTrue();
    }

    @Test
    void shouldExtractUsernameFromToken() {
        String token = tokenProvider.generateToken(authentication);
        String username = tokenProvider.getUsernameFromJWT(token);
        
        assertThat(username).isEqualTo("test@example.com");
    }

    @Test
    void shouldFailValidationForInvalidToken() {
        assertThat(tokenProvider.validateToken("invalid.token.string")).isFalse();
    }
}
