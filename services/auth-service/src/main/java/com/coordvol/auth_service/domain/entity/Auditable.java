package com.coordvol.auth_service.domain.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;

import lombok.Data;

@Data
public abstract class Auditable {

    @CreatedDate
    @Column("created_at")
    private LocalDateTime createdAt;
    @LastModifiedDate
    @Column("updated_at")
    private LocalDateTime updatedAt;
    @CreatedBy
    @Column("created_by")
    private String createdBy;
    @LastModifiedBy
    @Column("updated_by")
    private String updatedBy;
}
