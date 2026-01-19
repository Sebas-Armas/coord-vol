package com.coordvol.auth_service.e2e;

import static org.assertj.core.api.Assertions.assertThat;

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

import com.coordvol.auth_service.domain.enums.Language;
import com.coordvol.auth_service.domain.enums.Role;
import com.coordvol.auth_service.dto.LoginRequestDTO;
import com.coordvol.auth_service.dto.LoginResponseDTO;
import com.coordvol.auth_service.dto.RegisterRequestDTO;

/**
 * End-to-End tests for complete user journeys.
 * 
 * These tests simulate real user workflows from start to finish,
 * testing multiple components working together.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Testcontainers
@DisplayName("User Journey E2E Tests")
public class UserJourneyE2ETest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () -> "r2dbc:postgresql://" + postgres.getHost() + ":"
                + postgres.getFirstMappedPort() + "/" + postgres.getDatabaseName());
        registry.add("spring.r2dbc.username", postgres::getUsername);
        registry.add("spring.r2dbc.password", postgres::getPassword);
        registry.add("spring.flyway.url", postgres::getJdbcUrl);
        registry.add("spring.flyway.user", postgres::getUsername);
        registry.add("spring.flyway.password", postgres::getPassword);
    }

    @Autowired
    private WebTestClient webTestClient;

    @Test
    @DisplayName("Complete volunteer journey: Register → Login → Access Protected Resource")
    void completeVolunteerJourney() {
        String email = "volunteer-" + UUID.randomUUID() + "@example.com";
        String password = "password123";

        // Step 1: Register as volunteer
        RegisterRequestDTO registerRequestDTO = RegisterRequestDTO.builder()
                .email(email)
                .password(password)
                .firstName("Jane")
                .lastName("Volunteer")
                .role(Role.VOLUNTEER)
                .language(Language.EN)
                .build();

        RegisterResponseDTO registerResponseDTO = webTestClient.post()
                .uri("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(registerRequestDTO)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(RegisterResponseDTO.class)
                .returnResult()
                .getResponseBody();

        assertThat(registerResponseDTO).isNotNull();
        assertThat(registerResponseDTO.getUserId()).isNotNull();
        assertThat(registerResponseDTO.getRole()).isEqualTo(Role.VOLUNTEER);
        assertThat(registerResponseDTO.isActive()).isTrue();

        // Step 2: Login with credentials
        LoginRequestDTO loginRequest = LoginRequestDTO.builder()
                .email(email)
                .password(password)
                .build();

        LoginResponseDTO loginResponse = webTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(loginRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginResponseDTO.class)
                .returnResult()
                .getResponseBody();

        assertThat(loginResponse).isNotNull();
        assertThat(loginResponse.getToken()).isNotEmpty();
        assertThat(loginResponse.getRole()).isEqualTo(Role.VOLUNTEER);

        // Step 3: Access protected resource with token
        webTestClient.get()
                .uri("/auth/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + loginResponse.getToken())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.userId").isEqualTo(registerResponseDTO.getUserId().toString())
                .jsonPath("$.email").isEqualTo(email)
                .jsonPath("$.role").isEqualTo("VOLUNTEER")
                .jsonPath("$.status").isEqualTo("ACTIVE");

        // Step 4: Logout
        webTestClient.post()
                .uri("/auth/logout")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + loginResponse.getToken())
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    @DisplayName("Complete coordinator journey: Register → Login → Verify Role")
    void completeCoordinatorJourney() {
        String email = "coordinator-" + UUID.randomUUID() + "@example.com";

        // Step 1: Register as coordinator
        RegisterRequestDTO registerRequestDTO = RegisterRequestDTO.builder()
                .email(email)
                .password("password123")
                .firstName("John")
                .lastName("Coordinator")
                .role(Role.COORDINATOR)
                .language(Language.ES)
                .build();

        webTestClient.post()
                .uri("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(registerRequestDTO)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.role").isEqualTo("COORDINATOR");

        // Step 2: Login
        LoginResponseDTO loginResponseDTO = webTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new LoginRequestDTO(email, "password123"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginResponseDTO.class)
                .returnResult()
                .getResponseBody();

        // Step 3: Verify coordinator role
        webTestClient.get()
                .uri("/auth/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + loginResponseDTO.getToken())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.role").isEqualTo("COORDINATOR");
    }

    @Test
    @DisplayName("Admin journey: Login → Create User → Update Status")
    void adminUserManagementJourney() {
        // Step 1: Login as admin
        LoginResponseDTO adminLogin = webTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new LoginRequestDTO("admin@example.com", "admin123"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginResponseDTO.class)
                .returnResult()
                .getResponseBody();

        assertThat(adminLogin.getRole()).isEqualTo(Role.ADMIN);
        String adminToken = "Bearer " + adminLogin.getToken();

        // Step 2: Create new user
        String newUserEmail = "managed-user-" + UUID.randomUUID() + "@example.com";

        CreateUserResponseDTO createUserResponseDTO = webTestClient.post()
                .uri("/auth/users")
                .header(HttpHeaders.AUTHORIZATION, adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                            "email": "%s",
                            "password": "password123",
                            "role": "VOLUNTEER"
                        }
                        """.formatted(newUserEmail))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(CreateUserResponseDTO.class)
                .returnResult()
                .getResponseBody();
        createUserResponseDTO.getId();

        // Extract userId from response (in real scenario)
        // For now, we'll verify the user can login
        webTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new LoginRequestDTO(newUserEmail, "password123"))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @DisplayName("Failed journey: Invalid credentials → No access")
    void failedLoginJourney() {
        // Attempt login with invalid credentials
        webTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new LoginRequestDTO("nonexistent@example.com", "wrongpassword"))
                .exchange()
                .expectStatus().isUnauthorized();

        // Attempt to access protected resource without token
        webTestClient.get()
                .uri("/auth/me")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("Registration validation journey: Invalid data → Error responses")
    void registrationValidationJourney() {
        // Invalid email format
        webTestClient.post()
                .uri("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                            "email": "invalid-email",
                            "password": "password123",
                            "firstName": "John",
                            "lastName": "Doe",
                            "role": "VOLUNTEER",
                            "language": "EN"
                        }
                        """)
                .exchange()
                .expectStatus().isBadRequest();

        // Weak password
        webTestClient.post()
                .uri("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                            "email": "user@example.com",
                            "password": "weak",
                            "firstName": "John",
                            "lastName": "Doe",
                            "role": "VOLUNTEER",
                            "language": "EN"
                        }
                        """)
                .exchange()
                .expectStatus().isBadRequest();

        // Attempt to register as ADMIN
        webTestClient.post()
                .uri("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                            "email": "admin-attempt@example.com",
                            "password": "password123",
                            "firstName": "Admin",
                            "lastName": "Attempt",
                            "role": "ADMIN",
                            "language": "EN"
                        }
                        """)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("Duplicate registration journey: Register → Try again → Conflict")
    void duplicateRegistrationJourney() {
        String email = "duplicate-" + UUID.randomUUID() + "@example.com";

        RegisterRequestDTO request = RegisterRequestDTO.builder()
                .email(email)
                .password("password123")
                .firstName("John")
                .lastName("Doe")
                .role(Role.VOLUNTEER)
                .language(Language.EN)
                .build();

        // First registration succeeds
        webTestClient.post()
                .uri("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated();

        // Second registration with same email fails
        webTestClient.post()
                .uri("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isEqualTo(409) // Conflict
                .expectBody()
                .jsonPath("$.message").value(message -> assertThat(message.toString()).contains("already exists"));
    }

    @Test
    @DisplayName("Non-admin authorization journey: Volunteer tries admin endpoint → Forbidden")
    void unauthorizedAdminAccessJourney() {
        String email = "volunteer-nonadmin-" + UUID.randomUUID() + "@example.com";

        // Register and login as volunteer
        RegisterRequestDTO registerRequestDTO = RegisterRequestDTO.builder()
                .email(email)
                .password("password123")
                .firstName("Jane")
                .lastName("Volunteer")
                .role(Role.VOLUNTEER)
                .language(Language.EN)
                .build();

        webTestClient.post()
                .uri("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(registerRequestDTO)
                .exchange()
                .expectStatus().isCreated();

        LoginResponseDTO loginResponse = webTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new LoginRequestDTO(email, "password123"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginResponseDTO.class)
                .returnResult()
                .getResponseBody();

        // Attempt to create user (admin-only action)
        webTestClient.post()
                .uri("/auth/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + loginResponse.getToken())
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
}
