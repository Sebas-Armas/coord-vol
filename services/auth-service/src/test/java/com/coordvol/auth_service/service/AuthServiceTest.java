package com.coordvol.auth_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.coordvol.auth_service.domain.entity.AuthUser;
import com.coordvol.auth_service.domain.enums.Role;
import com.coordvol.auth_service.domain.enums.UserStatus;
import com.coordvol.auth_service.repository.AuthUserRepository;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Unit tests for AuthService.
 * 
 * These tests verify business logic in isolation using mocks.
 * No Spring context, no database - fast and focused.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
public class AuthServiceTest {

    @Mock
    private AuthUserRepository authUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserServiceClient userServiceClient;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(authUserRepository, passwordEncoder, jwtService, userServiceClient);
    }

    @Nested
    @DisplayName("Registration Tests")
    class RegistrationTests {

        @Test
        @DisplayName("Should register new user with valid data")
        void shouldRegisterUser_whenValidRequest() {
            // Arrange
            RegisterRequest request = createValidRegisterRequest();
            AuthUser savedUser = createAuthUser(request.getEmail(), request.getRole());

            when(authUserRepository.existsByEmail(request.getEmail())).thenReturn(Mono.just(false));
            when(passwordEncoder.encode(request.getPassword())).thenReturn("hashedPassword");
            when(authUserRepository.save(any(AuthUser.class))).thenReturn(Mono.just(savedUser));
            when(userServiceClient.createUserProfile(any(), anyString(), anyString(), anyString(), any()))
                    .thenReturn(Mono.empty());

            // Act & Assert
            StepVerifier.create(authService.register(request))
                    .assertNext(response -> {
                        assertThat(response).isNotNull();
                        assertThat(response.getUserId()).isEqualTo(savedUser.getId());
                        assertThat(response.getRole()).isEqualTo(Role.VOLUNTEER);
                        assertThat(response.isActive()).isTrue();
                        assertThat(response.getCreatedAt()).isNotNull();
                    })
                    .verifyComplete();

            // Verify interactions
            verify(authUserRepository).existsByEmail(request.getEmail());
            verify(passwordEncoder).encode(request.getPassword());
            verify(authUserRepository).save(any(AuthUser.class));
            verify(userServiceClient).createUserProfile(
                    eq(savedUser.getId()),
                    eq(request.getFirstName()),
                    eq(request.getLastName()),
                    eq(request.getEmail()),
                    eq(request.getLanguage()));
        }

        @Test
        @DisplayName("Should throw ConflictException when email already exists")
        void shouldThrowConflictException_whenEmailExists() {
            // Arrange
            RegisterRequest request = createValidRegisterRequest();
            when(authUserRepository.existsByEmail(request.getEmail())).thenReturn(Mono.just(true));

            // Act & Assert
            StepVerifier.create(authService.register(request))
                    .expectErrorMatches(throwable -> throwable instanceof ConflictException &&
                            throwable.getMessage().contains("Email already exists"))
                    .verify();

            verify(authUserRepository).existsByEmail(request.getEmail());
            verify(authUserRepository, never()).save(any());
            verify(userServiceClient, never()).createUserProfile(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Should throw BadRequestException when trying to register as ADMIN")
        void shouldThrowBadRequestException_whenAdminRole() {
            // Arrange
            RegisterRequest request = createValidRegisterRequest();
            request.setRole(Role.ADMIN);

            // Act & Assert
            StepVerifier.create(authService.register(request))
                    .expectErrorMatches(throwable -> throwable instanceof BadRequestException &&
                            throwable.getMessage().contains("Invalid role for registration"))
                    .verify();

            verify(authUserRepository, never()).existsByEmail(any());
            verify(authUserRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should register user as COORDINATOR role")
        void shouldRegisterUser_whenCoordinatorRole() {
            // Arrange
            RegisterRequest request = createValidRegisterRequest();
            request.setRole(Role.COORDINATOR);
            AuthUser savedUser = createAuthUser(request.getEmail(), Role.COORDINATOR);

            when(authUserRepository.existsByEmail(request.getEmail())).thenReturn(Mono.just(false));
            when(passwordEncoder.encode(request.getPassword())).thenReturn("hashedPassword");
            when(authUserRepository.save(any(AuthUser.class))).thenReturn(Mono.just(savedUser));
            when(userServiceClient.createUserProfile(any(), any(), any(), any(), any()))
                    .thenReturn(Mono.empty());

            // Act & Assert
            StepVerifier.create(authService.register(request))
                    .assertNext(response -> assertThat(response.getRole()).isEqualTo(Role.COORDINATOR))
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Login Tests")
    class LoginTests {

        @Test
        @DisplayName("Should return JWT token when credentials are valid")
        void shouldReturnToken_whenCredentialsValid() {
            // Arrange
            LoginRequest request = createValidLoginRequest();
            AuthUser user = createAuthUser(request.getEmail(), Role.COORDINATOR);

            when(authUserRepository.findByEmail(request.getEmail())).thenReturn(Mono.just(user));
            when(passwordEncoder.matches(request.getPassword(), user.getPasswordHash())).thenReturn(true);
            when(jwtService.generateToken(user.getId(), user.getRole())).thenReturn("jwt-token-123");
            when(jwtService.getExpirationTime()).thenReturn(86400000L);

            // Act & Assert
            StepVerifier.create(authService.login(request))
                    .assertNext(response -> {
                        assertThat(response.getAccessToken()).isEqualTo("jwt-token-123");
                        assertThat(response.getTokenType()).isEqualTo("Bearer");
                        assertThat(response.getExpiresIn()).isEqualTo(86400);
                        assertThat(response.getRole()).isEqualTo(Role.COORDINATOR);
                    })
                    .verifyComplete();

            verify(authUserRepository).findByEmail(request.getEmail());
            verify(passwordEncoder).matches(request.getPassword(), user.getPasswordHash());
            verify(jwtService).generateToken(user.getId(), user.getRole());
        }

        @Test
        @DisplayName("Should throw UnauthorizedException when user not found")
        void shouldThrowUnauthorizedException_whenUserNotFound() {
            // Arrange
            LoginRequest request = createValidLoginRequest();
            when(authUserRepository.findByEmail(request.getEmail())).thenReturn(Mono.empty());

            // Act & Assert
            StepVerifier.create(authService.login(request))
                    .expectErrorMatches(throwable -> throwable instanceof UnauthorizedException &&
                            throwable.getMessage().contains("Invalid credentials"))
                    .verify();

            verify(passwordEncoder, never()).matches(any(), any());
            verify(jwtService, never()).generateToken(any(), any());
        }

        @Test
        @DisplayName("Should throw UnauthorizedException when password is invalid")
        void shouldThrowUnauthorizedException_whenPasswordInvalid() {
            // Arrange
            LoginRequest request = createValidLoginRequest();
            AuthUser user = createAuthUser(request.getEmail(), Role.VOLUNTEER);

            when(authUserRepository.findByEmail(request.getEmail())).thenReturn(Mono.just(user));
            when(passwordEncoder.matches(request.getPassword(), user.getPasswordHash())).thenReturn(false);

            // Act & Assert
            StepVerifier.create(authService.login(request))
                    .expectError(UnauthorizedException.class)
                    .verify();

            verify(jwtService, never()).generateToken(any(), any());
        }

        @Test
        @DisplayName("Should throw UnauthorizedException when user is inactive")
        void shouldThrowUnauthorizedException_whenUserInactive() {
            // Arrange
            LoginRequest request = createValidLoginRequest();
            AuthUser user = createAuthUser(request.getEmail(), Role.VOLUNTEER);
            user.setStatus(UserStatus.INACTIVE);

            when(authUserRepository.findByEmail(request.getEmail())).thenReturn(Mono.just(user));

            // Act & Assert
            StepVerifier.create(authService.login(request))
                    .expectErrorMatches(throwable -> throwable instanceof UnauthorizedException &&
                            throwable.getMessage().contains("not active"))
                    .verify();

            verify(passwordEncoder, never()).matches(any(), any());
        }

        @Test
        @DisplayName("Should throw UnauthorizedException when user is deleted")
        void shouldThrowUnauthorizedException_whenUserDeleted() {
            // Arrange
            LoginRequest request = createValidLoginRequest();
            AuthUser user = createAuthUser(request.getEmail(), Role.VOLUNTEER);
            user.setStatus(UserStatus.DELETED);

            when(authUserRepository.findByEmail(request.getEmail())).thenReturn(Mono.just(user));

            // Act & Assert
            StepVerifier.create(authService.login(request))
                    .expectError(UnauthorizedException.class)
                    .verify();
        }
    }

    @Nested
    @DisplayName("Get Current User Tests")
    class GetCurrentUserTests {

        @Test
        @DisplayName("Should return user info when user exists")
        void shouldReturnUserInfo_whenUserExists() {
            // Arrange
            UUID userId = UUID.randomUUID();
            AuthUser user = createAuthUser("user@example.com", Role.COORDINATOR);
            user.setId(userId);

            when(authUserRepository.findById(userId)).thenReturn(Mono.just(user));

            // Act & Assert
            StepVerifier.create(authService.getCurrentUser(userId))
                    .assertNext(response -> {
                        assertThat(response.getUserId()).isEqualTo(userId);
                        assertThat(response.getEmail()).isEqualTo(user.getEmail());
                        assertThat(response.getRole()).isEqualTo(Role.COORDINATOR);
                        assertThat(response.isActive()).isTrue();
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should throw NotFoundException when user not found")
        void shouldThrowNotFoundException_whenUserNotFound() {
            // Arrange
            UUID userId = UUID.randomUUID();
            when(authUserRepository.findById(userId)).thenReturn(Mono.empty());

            // Act & Assert
            StepVerifier.create(authService.getCurrentUser(userId))
                    .expectError(NotFoundException.class)
                    .verify();
        }
    }

    @Nested
    @DisplayName("Create User (Admin) Tests")
    class CreateUserTests {

        @Test
        @DisplayName("Should create user when admin provides valid data")
        void shouldCreateUser_whenValidRequest() {
            // Arrange
            CreateUserRequest request = createValidCreateUserRequest();
            AuthUser savedUser = createAuthUser(request.getEmail(), request.getRole());

            when(authUserRepository.existsByEmail(request.getEmail())).thenReturn(Mono.just(false));
            when(passwordEncoder.encode(request.getPassword())).thenReturn("hashedPassword");
            when(authUserRepository.save(any(AuthUser.class))).thenReturn(Mono.just(savedUser));

            // Act & Assert
            StepVerifier.create(authService.createUser(request))
                    .assertNext(response -> {
                        assertThat(response.getEmail()).isEqualTo(request.getEmail());
                        assertThat(response.getRole()).isEqualTo(request.getRole());
                        assertThat(response.isActive()).isTrue();
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should throw BadRequestException when trying to create ADMIN")
        void shouldThrowBadRequestException_whenCreatingAdmin() {
            // Arrange
            CreateUserRequest request = createValidCreateUserRequest();
            request.setRole(Role.ADMIN);

            // Act & Assert
            StepVerifier.create(authService.createUser(request))
                    .expectErrorMatches(throwable -> throwable instanceof BadRequestException &&
                            throwable.getMessage().contains("Cannot create ADMIN users"))
                    .verify();

            verify(authUserRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw ConflictException when email exists")
        void shouldThrowConflictException_whenEmailExists() {
            // Arrange
            CreateUserRequest request = createValidCreateUserRequest();
            when(authUserRepository.existsByEmail(request.getEmail())).thenReturn(Mono.just(true));

            // Act & Assert
            StepVerifier.create(authService.createUser(request))
                    .expectError(ConflictException.class)
                    .verify();
        }
    }

    @Nested
    @DisplayName("Update User Status Tests")
    class UpdateStatusTests {

        @Test
        @DisplayName("Should update user status when user exists")
        void shouldUpdateStatus_whenUserExists() {
            // Arrange
            UUID userId = UUID.randomUUID();
            UpdateStatusRequest request = new UpdateStatusRequest(UserStatus.INACTIVE);
            AuthUser user = createAuthUser("user@example.com", Role.VOLUNTEER);
            user.setId(userId);

            when(authUserRepository.findById(userId)).thenReturn(Mono.just(user));
            when(authUserRepository.save(any(AuthUser.class))).thenAnswer(invocation -> {
                AuthUser savedUser = invocation.getArgument(0);
                return Mono.just(savedUser);
            });

            // Act & Assert
            StepVerifier.create(authService.updateUserStatus(userId, request))
                    .assertNext(response -> {
                        assertThat(response.getUserId()).isEqualTo(userId);
                        assertThat(response.getStatus()).isEqualTo(UserStatus.INACTIVE);
                    })
                    .verifyComplete();

            verify(authUserRepository).findById(userId);
            verify(authUserRepository).save(argThat(u -> u.getStatus() == UserStatus.INACTIVE));
        }

        @Test
        @DisplayName("Should throw NotFoundException when user not found")
        void shouldThrowNotFoundException_whenUserNotFound() {
            // Arrange
            UUID userId = UUID.randomUUID();
            UpdateStatusRequest request = new UpdateStatusRequest(UserStatus.DELETED);

            when(authUserRepository.findById(userId)).thenReturn(Mono.empty());

            // Act & Assert
            StepVerifier.create(authService.updateUserStatus(userId, request))
                    .expectError(NotFoundException.class)
                    .verify();

            verify(authUserRepository, never()).save(any());
        }
    }

    // Helper methods for test data creation

    private RegisterRequest createValidRegisterRequest() {
        return RegisterRequest.builder()
                .email("test@example.com")
                .password("password123")
                .firstName("John")
                .lastName("Doe")
                .role(Role.VOLUNTEER)
                .language(Language.EN)
                .build();
    }

    private LoginRequest createValidLoginRequest() {
        return LoginRequest.builder()
                .email("test@example.com")
                .password("password123")
                .build();
    }

    private CreateUserRequest createValidCreateUserRequest() {
        return CreateUserRequest.builder()
                .email("newuser@example.com")
                .password("password123")
                .role(Role.COORDINATOR)
                .build();
    }

    private AuthUser createAuthUser(String email, Role role) {
        return AuthUser.builder()
                .id(UUID.randomUUID())
                .email(email)
                .passwordHash("$2a$10$hashedPassword")
                .role(role)
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
