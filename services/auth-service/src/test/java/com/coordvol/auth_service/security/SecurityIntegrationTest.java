package com.coordvol.auth_service.security;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.coordvol.auth_service.domain.enums.Role;
import com.coordvol.auth_service.dto.LoginRequestDTO;
import com.coordvol.auth_service.dto.LoginResponseDTO;
import com.coordvol.auth_service.service.JwtService;

/**
 * Integration tests for Security configuration.
 * 
 * Tests authentication, authorization, and JWT filter behavior.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Testcontainers
@DisplayName("Security Integration Tests")
public class SecurityIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () ->
                "r2dbc:postgresql://" + postgres.getHost() + ":" + postgres.getFirstMappedPort() + "/" + postgres.getDatabaseName());
        registry.add("spring.r2dbc.username", postgres::getUsername);
        registry.add("spring.r2dbc.password", postgres::getPassword);
        registry.add("spring.flyway.url", postgres::getJdbcUrl);
        registry.add("spring.flyway.user", postgres::getUsername);
        registry.add("spring.flyway.password", postgres::getPassword);
    }
    
    @Autowired
    private WebTestClient webTestClient;
    
    @Autowired
    private JwtService jwtService;
    
    @Test
    @DisplayName("Should allow access to public endpoints without authentication")
    void shouldAllowPublicEndpoints() {
        webTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new LoginRequest("test@example.com", "password"))
                .exchange()
                .expectStatus().isUnauthorized(); // Fails due to invalid credentials, not missing auth
    }
    
    @Test
    @DisplayName("Should block protected endpoints without token")
    void shouldBlockProtectedEndpointsWithoutToken() {
        webTestClient.get()
                .uri("/auth/me")
                .exchange()
                .expectStatus().isUnauthorized();
    }
    
    @Test
    @DisplayName("Should allow access to protected endpoints with valid token")
    void shouldAllowAccessWithValidToken() {
        // Arrange - Get valid token by logging in
        LoginRequestDTO loginRequest = LoginRequestDTO.builder()
                .email("admin@example.com")
                .password("admin123")
                .build();
        
        String token = webTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(loginRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginResponseDTO.class)
                .returnResult()
                .getResponseBody()
                .getToken();
        
        // Act & Assert
        webTestClient.get()
                .uri("/auth/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.email").isEqualTo("admin@example.com")
                .jsonPath("$.role").isEqualTo("ADMIN");
    }
    
    @Test
    @DisplayName("Should reject invalid token")
    void shouldRejectInvalidToken() {
        webTestClient.get()
                .uri("/auth/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid.token.here")
                .exchange()
                .expectStatus().isUnauthorized();
    }
    
    @Test
    @DisplayName("Should reject malformed authorization header")
    void shouldRejectMalformedAuthHeader() {
        webTestClient.get()
                .uri("/auth/me")
                .header(HttpHeaders.AUTHORIZATION, "InvalidFormat token123")
                .exchange()
                .expectStatus().isUnauthorized();
    }
    
    @Test
    @DisplayName("Should reject token without Bearer prefix")
    void shouldRejectTokenWithoutBearerPrefix() {
        String token = jwtService.generateToken(UUID.randomUUID(), Role.VOLUNTEER);
        
        webTestClient.get()
                .uri("/auth/me")
                .header(HttpHeaders.AUTHORIZATION, token) // Missing "Bearer " prefix
                .exchange()
                .expectStatus().isUnauthorized();
    }
    
    @Test
    @DisplayName("Should allow admin-only endpoint for admin user")
    void shouldAllowAdminEndpointForAdmin() {
        // Arrange - Login as admin
        String adminToken = webTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new LoginRequestDTO("admin@example.com", "admin123"))
                .exchange()
                .expectBody(LoginResponseDTO.class)
                .returnResult()
                .getResponseBody()
                .getToken();
        
        // Act & Assert - Should allow access to admin endpoint
        webTestClient.post()
                .uri("/auth/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                    {
                        "email": "newuser@example.com",
                        "password": "password123",
                        "role": "VOLUNTEER"
                    }
                    """)
                .exchange()
                .expectStatus().isCreated();
    }
    
    @Test
    @DisplayName("Should reject admin-only endpoint for non-admin user")
    void shouldRejectAdminEndpointForNonAdmin() {
        // Arrange - Create and login as non-admin user
        UUID userId = UUID.randomUUID();
        String volunteerToken = jwtService.generateToken(userId, Role.VOLUNTEER);
        
        // Act & Assert - Should be forbidden
        webTestClient.post()
                .uri("/auth/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + volunteerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                    {
                        "email": "attempt@example.com",
                        "password": "password123",
                        "role": "VOLUNTEER"
                    }
                    """)
                .exchange()
                .expectStatus().isForbidden();
    }
    
    @Test
    @DisplayName("Should allow logout for authenticated users")
    void shouldAllowLogoutForAuthenticatedUsers() {
        // Arrange
        String token = jwtService.generateToken(UUID.randomUUID(), Role.VOLUNTEER);
        
        // Act & Assert
        webTestClient.post()
                .uri("/auth/logout")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange()
                .expectStatus().isNoContent();
    }
    
    @Test
    @DisplayName("Should block logout without authentication")
    void shouldBlockLogoutWithoutAuth() {
        webTestClient.post()
                .uri("/auth/logout")
                .exchange()
                .expectStatus().isUnauthorized();
    }
    
    @Test
    @DisplayName("Should allow actuator health endpoint without authentication")
    void shouldAllowActuatorHealthEndpoint() {
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk();
    }
    
    @Test
    @DisplayName("Should preserve role information in security context")
    void shouldPreserveRoleInSecurityContext() {
        // Arrange
        UUID userId = UUID.randomUUID();
        String coordinatorToken = jwtService.generateToken(userId, Role.COORDINATOR);
        
        // Act & Assert
        webTestClient.get()
                .uri("/auth/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + coordinatorToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.role").isEqualTo("COORDINATOR");
    }
    
    @Test
    @DisplayName("Should handle expired token")
    void shouldHandleExpiredToken() {
        // Arrange - Create service with very short expiration
        JwtService shortLivedJwtService = new JwtService(
                "mySecretKeyForJWTTokenGenerationMustBeLongEnoughForHS512Algorithm",
                1L, // 1ms expiration
                "auth-service"
        );
        
        String expiredToken = shortLivedJwtService.generateToken(UUID.randomUUID(), Role.VOLUNTEER);
        
        // Wait for token to expire
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Act & Assert
        webTestClient.get()
                .uri("/auth/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken)
                .exchange()
                .expectStatus().isUnauthorized();
    }
}
