package com.coordvol.auth_service.service;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.coordvol.auth_service.entity.AuthUser;
import com.coordvol.auth_service.repository.AuthUserRepository;
import com.coordvol.auth_service.service.impl.AuthServiceImpl;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private AuthUserRepository authUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthServiceImpl authService;

    private AuthUser testAuthUser;
    private LoginRequestDTO loginRequest;
    private RegisterRequestDTO registerRequest;
}
