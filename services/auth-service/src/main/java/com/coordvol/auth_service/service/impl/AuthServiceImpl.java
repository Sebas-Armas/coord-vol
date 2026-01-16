package com.coordvol.auth_service.service.impl;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.coordvol.auth_service.repository.AuthUserRepository;

@Service
public class AuthServiceImpl {
    private final AuthUserRepository authUserRepository;
    private final PasswordEncoder passwordEncoder;
}
