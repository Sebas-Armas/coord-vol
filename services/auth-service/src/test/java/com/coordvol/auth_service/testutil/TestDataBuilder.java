package com.coordvol.auth_service.testutil;

import java.time.LocalDateTime;
import java.util.UUID;

import com.coordvol.auth_service.domain.entity.AuthUser;
import com.coordvol.auth_service.domain.enums.Language;
import com.coordvol.auth_service.domain.enums.Role;
import com.coordvol.auth_service.domain.enums.UserStatus;
import com.coordvol.auth_service.dto.LoginRequestDTO;
import com.coordvol.auth_service.dto.RegisterRequestDTO;

/**
 * Test Data Builder utility class.
 * 
 * Provides convenient methods to create test data with sensible defaults.
 * Reduces boilerplate in tests and makes them more readable.
 * 
 * Usage:
 * 
 * <pre>
 * AuthUser user = TestDataBuilder.aVolunteer().build();
 * RegisterRequest request = TestDataBuilder.aRegisterRequest().withEmail("custom@email.com").build();
 * </pre>
 */
public class TestDataBuilder {
    // ==================== AuthUser Builders ====================

    public static AuthUser.AuthUserBuilder anAuthUser() {
        return AuthUser.builder()
                .id(UUID.randomUUID())
                .email("user@example.com")
                .passwordHash("$2a$10$hashedPassword123456789")
                .role(Role.VOLUNTEER)
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now());
    }

    public static AuthUser.AuthUserBuilder aVolunteer() {
        return anAuthUser()
                .email("volunteer@example.com")
                .role(Role.VOLUNTEER);
    }

    public static AuthUser.AuthUserBuilder aCoordinator() {
        return anAuthUser()
                .email("coordinator@example.com")
                .role(Role.COORDINATOR);
    }

    public static AuthUser.AuthUserBuilder anAdmin() {
        return anAuthUser()
                .email("admin@example.com")
                .role(Role.ADMIN);
    }

    public static AuthUser.AuthUserBuilder anInactiveUser() {
        return anAuthUser()
                .status(UserStatus.INACTIVE);
    }

    public static AuthUser.AuthUserBuilder aDeletedUser() {
        return anAuthUser()
                .status(UserStatus.DELETED);
    }

    // ==================== Request Builders ====================

    public static RegisterRequestDTO.RegisterRequestDTOBuilder aRegisterRequest() {
        return RegisterRequestDTO.builder()
                .email("register@example.com")
                .password("password123")
                .firstName("John")
                .lastName("Doe")
                .role(Role.VOLUNTEER)
                .language(Language.ES);
    }

    public static RegisterRequestDTO.RegisterRequestDTOBuilder aVolunteerRegistration() {
        return aRegisterRequest()
                .role(Role.VOLUNTEER);
    }

    public static RegisterRequestDTO.RegisterRequestDTOBuilder aCoordinatorRegistration() {
        return aRegisterRequest()
                .role(Role.COORDINATOR);
    }

    public static LoginRequestDTO.LoginRequestDTOBuilder aLoginRequest() {
        return LoginRequestDTO.builder()
                .email("login@example.com")
                .password("password123");
    }

    public static CreateUserRequestDTO.CreateUserRequestDTOBuilder aCreateUserRequest() {
        return CreateUserRequestDTO.builder()
                .email("newuser@example.com")
                .password("password123")
                .role(Role.VOLUNTEER);
    }

    public static UpdateStatusRequestDTO aStatusUpdate(UserStatus status) {
        return new UpdateStatusRequestd(status);
    }

    public static UpdateStatusRequestDTO anInactiveStatusUpdate() {
        return aStatusUpdate(UserStatus.INACTIVE);
    }

    public static UpdateStatusRequestDTO aDeletedStatusUpdate() {
        return aStatusUpdate(UserStatus.DELETED);
    }

    public static UpdateStatusRequestDTO anActiveStatusUpdate() {
        return aStatusUpdate(UserStatus.ACTIVE);
    }

    // ==================== Custom Builders with Fluent API ====================

    /**
     * Fluent builder for RegisterRequest with custom methods.
     */
    public static class RegisterRequestFluentBuilder {
        private final RegisterRequestDTO.RegisterRequestDTOBuilder builder;

        private RegisterRequestFluentBuilder() {
            this.builder = aRegisterRequest();
        }

        public static RegisterRequestFluentBuilder registration() {
            return new RegisterRequestFluentBuilder();
        }

        public RegisterRequestFluentBuilder withEmail(String email) {
            builder.email(email);
            return this;
        }

        public RegisterRequestFluentBuilder withPassword(String password) {
            builder.password(password);
            return this;
        }

        public RegisterRequestFluentBuilder withName(String firstName, String lastName) {
            builder.firstName(firstName).lastName(lastName);
            return this;
        }

        public RegisterRequestFluentBuilder asVolunteer() {
            builder.role(Role.VOLUNTEER);
            return this;
        }

        public RegisterRequestFluentBuilder asCoordinator() {
            builder.role(Role.COORDINATOR);
            return this;
        }

        public RegisterRequestFluentBuilder withLanguage(Language language) {
            builder.language(language);
            return this;
        }

        public RegisterRequestDTO build() {
            return builder.build();
        }
    }

    /**
     * Fluent builder for AuthUser with custom methods.
     */
    public static class AuthUserFluentBuilder {
        private final AuthUser.AuthUserBuilder builder;

        private AuthUserFluentBuilder() {
            this.builder = anAuthUser();
        }

        public static AuthUserFluentBuilder user() {
            return new AuthUserFluentBuilder();
        }

        public AuthUserFluentBuilder withId(UUID id) {
            builder.id(id);
            return this;
        }

        public AuthUserFluentBuilder withEmail(String email) {
            builder.email(email);
            return this;
        }

        public AuthUserFluentBuilder withPasswordHash(String passwordHash) {
            builder.passwordHash(passwordHash);
            return this;
        }

        public AuthUserFluentBuilder withRole(Role role) {
            builder.role(role);
            return this;
        }

        public AuthUserFluentBuilder asVolunteer() {
            builder.role(Role.VOLUNTEER);
            return this;
        }

        public AuthUserFluentBuilder asCoordinator() {
            builder.role(Role.COORDINATOR);
            return this;
        }

        public AuthUserFluentBuilder asAdmin() {
            builder.role(Role.ADMIN);
            return this;
        }

        public AuthUserFluentBuilder withStatus(UserStatus status) {
            builder.status(status);
            return this;
        }

        public AuthUserFluentBuilder active() {
            builder.status(UserStatus.ACTIVE);
            return this;
        }

        public AuthUserFluentBuilder inactive() {
            builder.status(UserStatus.INACTIVE);
            return this;
        }

        public AuthUserFluentBuilder deleted() {
            builder.status(UserStatus.DELETED);
            return this;
        }

        public AuthUser build() {
            return builder.build();
        }
    }

    // ==================== Utility Methods ====================

    /**
     * Generates a unique email address for testing.
     */
    public static String uniqueEmail() {
        return "test-" + UUID.randomUUID() + "@example.com";
    }

    /**
     * Generates a unique email with prefix.
     */
    public static String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@example.com";
    }

    /**
     * Standard test password.
     */
    public static String testPassword() {
        return "password123";
    }

    /**
     * BCrypt hashed version of testPassword().
     */
    public static String testPasswordHash() {
        return "$2a$10$7xKGKZt1YRLNvVqQZVH4x.h3PmqVqC8qTJZ7YZGbFLLKBKZ3RZLlW";
    }
}
