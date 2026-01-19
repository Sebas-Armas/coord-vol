package com.coordvol.auth_service.service;

import java.util.Date;
import java.util.UUID;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.coordvol.auth_service.domain.enums.Role;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for JwtService.
 * 
 * Tests JWT token generation, validation, and claims extraction.
 */
@DisplayName("JWT Service Test")
public class JwtServiceTest {

    private JwtService jwtService;
    private final String secret = "mySecretKeyForJWTTokenGenerationMustBeLongEnoughForHS512Algorithm"; // 256-bit key
    private final long expiration = 3600000L; // 1 hour
    private final String issuer = "auth-service-test"; // 24 hours

    @BeforeEach
    void setUp() {
        jwtService = new JwtServiceImpl(secret, expiration, issuer);
    }

    @Nested
    @DisplayName("Token Generation Tests")
    class TokenGenerationTest {

        @Test
        @DisplayName("Should generate a valid JWT token")
        void shouldGenerateValidToken() {
            UUID userId = UUID.randomUUID();
            Role role = Role.COORDINATOR;

            String token = jwtService.generateToken(userId, role);
            
            assertThat(token).isNotNull()
                    .isNotEmpty();
            assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts    
        }

        @Test
        @DisplayName("Should include userId in token subject")
        void shouldIncludeUserIdInSubject() {
            UUID userId = UUID.randomUUID();
            Role role = Role.VOLUNTEER;
            
            // Act
            String token = jwtService.generateToken(userId, role);
            Claims claims = jwtService.extractClaims(token);
            
            // Assert
            assertThat(claims.getSubject()).isEqualTo(userId.toString());
        }

        @Test
        @DisplayName("Should include role in token claims")
        void shouldIncludeRoleInClaims() {
            // Arrange
            UUID userId = UUID.randomUUID();
            Role role = Role.ADMIN;
            
            // Act
            String token = jwtService.generateToken(userId, role);
            Claims claims = jwtService.extractClaims(token);
            
            // Assert
            assertThat(claims.get("role", String.class)).isEqualTo("ADMIN");
        }

        @Test
        @DisplayName("Should include issuer in token")
        void shouldIncludeIssuer() {
            // Arrange
            UUID userId = UUID.randomUUID();
            
            // Act
            String token = jwtService.generateToken(userId, Role.VOLUNTEER);
            Claims claims = jwtService.extractClaims(token);
            
            // Assert
            assertThat(claims.getIssuer()).isEqualTo(issuer);
        }
        
        @Test
        @DisplayName("Should set expiration time correctly")
        void shouldSetExpirationTime() {
            // Arrange
            UUID userId = UUID.randomUUID();
            long beforeGeneration = System.currentTimeMillis();
            
            // Act
            String token = jwtService.generateToken(userId, Role.COORDINATOR);
            Claims claims = jwtService.extractClaims(token);
            long afterGeneration = System.currentTimeMillis();
            
            // Assert
            Date expirationDate = claims.getExpiration();
            long expectedExpiration = beforeGeneration + expiration;
            assertThat(expirationDate.getTime()).isBetween(
                    expectedExpiration - 1000, // Allow 1 second tolerance
                    afterGeneration + expiration + 1000
            );
        }
    }

    @Nested
    @DisplayName("Token Validation Tests")
    class TokenValidationTest {

        @Test
        @DisplayName("Should validate correct token")
        void shouldValidateCorrectToken() {
            // Arrange
            UUID userId = UUID.randomUUID();
            String token = jwtService.generateToken(userId, Role.VOLUNTEER);
            
            // Act
            boolean isValid = jwtService.validateToken(token);
            
            // Assert
            assertThat(isValid).isTrue();
        }
        
        @Test
        @DisplayName("Should reject token with invalid signature")
        void shouldRejectInvalidSignature() {
            // Arrange
            UUID userId = UUID.randomUUID();
            String token = jwtService.generateToken(userId, Role.COORDINATOR);
            
            // Tamper with token (change last character)
            String tamperedToken = token.substring(0, token.length() - 1) + "X";
            
            // Act
            boolean isValid = jwtService.validateToken(tamperedToken);
            
            // Assert
            assertThat(isValid).isFalse();
        }
        
        @Test
        @DisplayName("Should reject malformed token")
        void shouldRejectMalformedToken() {
            // Act
            boolean isValid = jwtService.validateToken("not.a.valid.token");
            
            // Assert
            assertThat(isValid).isFalse();
        }
        
        @Test
        @DisplayName("Should reject expired token")
        void shouldRejectExpiredToken() {
            // Arrange - Create service with very short expiration
            JwtService shortLivedJwtService = new JwtService(secret, 1L, issuer); // 1ms expiration
            UUID userId = UUID.randomUUID();
            String token = shortLivedJwtService.generateToken(userId, Role.VOLUNTEER);
            
            // Wait for token to expire
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // Act
            boolean isValid = shortLivedJwtService.validateToken(token);
            
            // Assert
            assertThat(isValid).isFalse();
        }
    }
}
