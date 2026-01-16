package com.coordvol.auth_service.service;

import java.util.Date;

public interface JwtService {
    String generateToken(String username, String role);
    String generateRefreshToken(String username);
    String extractUsername(String token);
    String extractRole(String token);
    Date extractExpiration(String token);
    Boolean isTokenExpired(String token);
    Boolean validateToken(String token, String username);
}
