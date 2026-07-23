package com.microfinance.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.stream.Collectors;

/**
 * Utility class for generating and validating JSON Web Tokens (JWT).
 */
@Component
@Slf4j
public class JwtTokenProvider {

    private final SecretKey jwtSecretKey;
    private final long jwtExpirationMs;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String jwtSecret,
            @Value("${app.jwt.expiration-ms}") long jwtExpirationMs) {
        // Decode Base64 string into a robust SecretKey for HS256
        this.jwtSecretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
        this.jwtExpirationMs = jwtExpirationMs;
    }

    /**
     * Generate a signed JWT token for the authenticated user.
     *
     * @param authentication the successful authentication object
     * @return signed JWT string
     */
    public String generateToken(Authentication authentication) {
        String username = authentication.getName();
        
        // Extract the role (e.g., ROLE_APPLICANT)
        String role = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .subject(username)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(jwtSecretKey, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Extract the username (email) from the JWT token.
     *
     * @param token the JWT string
     * @return the subject (email)
     */
    public String getUsernameFromJWT(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(jwtSecretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getSubject();
    }

    /**
     * Validate the JWT token signature and expiration.
     *
     * @param authToken the JWT string
     * @return true if valid, false otherwise
     */
    public boolean validateToken(String authToken) {
        try {
            Jwts.parser().verifyWith(jwtSecretKey).build().parseSignedClaims(authToken);
            return true;
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token");
        } catch (ExpiredJwtException ex) {
            log.error("Expired JWT token");
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token");
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty.");
        }
        return false;
    }
}
