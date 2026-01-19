package com.coordvol.auth_service.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.coordvol.auth_service.entity.AuthUser;

import reactor.core.publisher.Mono;

public interface AuthUserRepository extends ReactiveCrudRepository<AuthUser, Long> {
    Mono<AuthUser> findByEmail(String email);
}
