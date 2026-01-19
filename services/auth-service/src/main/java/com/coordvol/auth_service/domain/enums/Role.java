package com.coordvol.auth_service.domain.enums;

public enum Role {
    ADMIN, 
    COORDINATOR, 
    VOLUNTEER;

    public static boolean isValidRegistrationRole(Role role) {
        return role == COORDINATOR || role == VOLUNTEER;
    }

}
