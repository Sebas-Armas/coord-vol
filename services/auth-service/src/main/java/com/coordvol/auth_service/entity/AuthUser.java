package com.coordvol.auth_service.entity;
import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("auth_users")
public class AuthUser {

    @Id
    private Long id;
    private String email;
    private String passwordHash;
    private String role;
    private boolean isActive;
    private LocalDateTime lastLogin;
}