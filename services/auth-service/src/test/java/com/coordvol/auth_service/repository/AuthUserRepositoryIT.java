package com.coordvol.auth_service.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.coordvol.auth_service.domain.entity.AuthUser;
import com.coordvol.auth_service.domain.enums.Role;
import com.coordvol.auth_service.domain.enums.UserStatus;

import reactor.test.StepVerifier;


/**
 * Integration tests for AuthUserRepository.
 * 
 * Uses Testcontainers to spin up a real PostgreSQL database.
 * Tests actual database operations and queries.
 */
@DataR2dbcTest
@Testcontainers
@DisplayName("AuthUserRepository Integration Tests")
public class AuthUserRepositoryIT {

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
    private AuthUserRepository repository;

    @AfterEach
    void cleanup() {
        // Clean up test data after each test
        repository.deleteAll().block();
    }

    @DisplayName("Should save and retrieve user")
    void shouldSaveAndRetrieveUser() {
        // Arrange
        AuthUser user = createTestUser("test@example.com", Role.VOLUNTEER);
        
        // Act & Assert
        StepVerifier.create(repository.save(user))
                .assertNext(savedUser -> {
                    assertThat(savedUser.getId()).isNotNull();
                    assertThat(savedUser.getEmail()).isEqualTo("test@example.com");
                    assertThat(savedUser.getRole()).isEqualTo(Role.VOLUNTEER);
                    assertThat(savedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
                })
                .verifyComplete();
    }
    
    @Test
    @DisplayName("Should find user by email")
    void shouldFindUserByEmail() {
        // Arrange
        AuthUser user = createTestUser("findme@example.com", Role.COORDINATOR);
        repository.save(user).block();
        
        // Act & Assert
        StepVerifier.create(repository.findByEmail("findme@example.com"))
                .assertNext(foundUser -> {
                    assertThat(foundUser.getEmail()).isEqualTo("findme@example.com");
                    assertThat(foundUser.getRole()).isEqualTo(Role.COORDINATOR);
                })
                .verifyComplete();
    }
    
    @Test
    @DisplayName("Should return empty when user not found by email")
    void shouldReturnEmptyWhenNotFoundByEmail() {
        // Act & Assert
        StepVerifier.create(repository.findByEmail("nonexistent@example.com"))
                .verifyComplete();
    }
    
    @Test
    @DisplayName("Should check if email exists")
    void shouldCheckEmailExists() {
        // Arrange
        AuthUser user = createTestUser("exists@example.com", Role.VOLUNTEER);
        repository.save(user).block();
        
        // Act & Assert
        StepVerifier.create(repository.existsByEmail("exists@example.com"))
                .expectNext(true)
                .verifyComplete();
        
        StepVerifier.create(repository.existsByEmail("notexists@example.com"))
                .expectNext(false)
                .verifyComplete();
    }
    
    @Test
    @DisplayName("Should enforce unique email constraint")
    void shouldEnforceUniqueEmail() {
        // Arrange
        AuthUser user1 = createTestUser("unique@example.com", Role.VOLUNTEER);
        AuthUser user2 = createTestUser("unique@example.com", Role.COORDINATOR);
        
        // Act - Save first user
        repository.save(user1).block();
        
        // Assert - Second save with same email should fail
        StepVerifier.create(repository.save(user2))
                .expectError()
                .verify();
    }
    
    @Test
    @DisplayName("Should update user status")
    void shouldUpdateUserStatus() {
        // Arrange
        AuthUser user = createTestUser("update@example.com", Role.VOLUNTEER);
        AuthUser savedUser = repository.save(user).block();
        
        // Act
        savedUser.setStatus(UserStatus.INACTIVE);
        
        // Assert
        StepVerifier.create(repository.save(savedUser))
                .assertNext(updated -> {
                    assertThat(updated.getStatus()).isEqualTo(UserStatus.INACTIVE);
                })
                .verifyComplete();
        
        // Verify in database
        StepVerifier.create(repository.findById(savedUser.getId()))
                .assertNext(found -> {
                    assertThat(found.getStatus()).isEqualTo(UserStatus.INACTIVE);
                })
                .verifyComplete();
    }
    
    @Test
    @DisplayName("Should delete user")
    void shouldDeleteUser() {
        // Arrange
        AuthUser user = createTestUser("delete@example.com", Role.COORDINATOR);
        AuthUser savedUser = repository.save(user).block();
        
        // Act
        StepVerifier.create(repository.deleteById(savedUser.getId()))
                .verifyComplete();
        
        // Assert
        StepVerifier.create(repository.findById(savedUser.getId()))
                .verifyComplete();
    }
    
    @Test
    @DisplayName("Should count all users")
    void shouldCountUsers() {
        // Arrange
        repository.save(createTestUser("user1@example.com", Role.VOLUNTEER)).block();
        repository.save(createTestUser("user2@example.com", Role.COORDINATOR)).block();
        repository.save(createTestUser("user3@example.com", Role.ADMIN)).block();
        
        // Act & Assert
        StepVerifier.create(repository.count())
                .expectNext(3L)
                .verifyComplete();
    }
    
    @Test
    @DisplayName("Should find all users")
    void shouldFindAllUsers() {
        // Arrange
        repository.save(createTestUser("all1@example.com", Role.VOLUNTEER)).block();
        repository.save(createTestUser("all2@example.com", Role.COORDINATOR)).block();
        
        // Act & Assert
        StepVerifier.create(repository.findAll())
                .expectNextCount(2)
                .verifyComplete();
    }
    
    @Test
    @DisplayName("Should handle different role types")
    void shouldHandleDifferentRoles() {
        // Test all role types
        for (Role role : Role.values()) {
            // Arrange
            AuthUser user = createTestUser(role.name().toLowerCase() + "@example.com", role);
            
            // Act & Assert
            StepVerifier.create(repository.save(user))
                    .assertNext(saved -> {
                        assertThat(saved.getRole()).isEqualTo(role);
                    })
                    .verifyComplete();
        }
    }
    
    @Test
    @DisplayName("Should handle different status types")
    void shouldHandleDifferentStatuses() {
        // Test all status types
        for (UserStatus status : UserStatus.values()) {
            // Arrange
            AuthUser user = createTestUser(status.name().toLowerCase() + "@example.com", Role.VOLUNTEER);
            user.setStatus(status);
            
            // Act & Assert
            StepVerifier.create(repository.save(user))
                    .assertNext(saved -> {
                        assertThat(saved.getStatus()).isEqualTo(status);
                    })
                    .verifyComplete();
        }
    }
    
    @Test
    @DisplayName("Should preserve password hash")
    void shouldPreservePasswordHash() {
        // Arrange
        String passwordHash = "$2a$10$hashedPasswordExample123456789";
        AuthUser user = createTestUser("password@example.com", Role.VOLUNTEER);
        user.setPasswordHash(passwordHash);
        
        // Act
        AuthUser savedUser = repository.save(user).block();
        
        // Assert
        StepVerifier.create(repository.findById(savedUser.getId()))
                .assertNext(found -> {
                    assertThat(found.getPasswordHash()).isEqualTo(passwordHash);
                })
                .verifyComplete();
    }
    
    // Helper method
    private AuthUser createTestUser(String email, Role role) {
        return AuthUser.builder()
                .email(email)
                .passwordHash("$2a$10$testHash")
                .role(role)
                .status(UserStatus.ACTIVE)
                .build();
    }
}
