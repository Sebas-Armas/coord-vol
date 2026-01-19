package com.coordvol.auth_service.service;

import java.util.Date;
import java.util.UUID;

import com.coordvol.auth_service.domain.enums.Role;

public interface JwtService {
    String generateToken(UUID userId, Role role);
    String generateRefreshToken(String username);
    UUID extractUserId(String token);
    Role extractRole(String token);
    Date extractExpiration(String token);
    String extractIssuer(String token);
    Boolean isTokenExpired(String token);
    Boolean validateToken(String token);
}
