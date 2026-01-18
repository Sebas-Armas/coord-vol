package com.coordvol.auth_service.domain.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import com.coordvol.auth_service.domain.enums.Role;
import com.coordvol.auth_service.domain.enums.UserStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("auth_users")
public class AuthUser extends Auditable {

    @Id
    private UUID id;
    private String email;
    @Column("password_hash")
    private String passwordHash;
    private Role role;
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;
    private LocalDateTime lastLogin;
}