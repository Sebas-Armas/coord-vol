package com.coordvol.auth_service.service;

import com.coordvol.auth_service.entity.AuthUser;

import reactor.core.publisher.Mono;

public interface AuthService {

    Mono<LoginResponseDTO> login(LoginRequestDTO request);
    Mono<AuthUser> register(RegisterRequestDTO request);
    Mono<LoginResponseDTO> refreshToken(String refreshToken);
}
