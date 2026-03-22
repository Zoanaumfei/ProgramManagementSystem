package com.oryzem.programmanagementsystem.platform.authorization;

import java.util.Locale;
import java.util.Optional;

public enum Role {
    ADMIN,
    MANAGER,
    MEMBER,
    SUPPORT,
    AUDITOR;

    public static Optional<Role> fromAuthority(String authority) {
        if (authority == null || authority.isBlank() || !authority.startsWith("ROLE_")) {
            return Optional.empty();
        }

        String normalized = authority.substring("ROLE_".length()).trim().toUpperCase(Locale.ROOT);
        try {
            return Optional.of(Role.valueOf(normalized));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    public boolean canManageRole(Role targetRole) {
        if (targetRole == null) {
            return false;
        }

        return switch (this) {
            case ADMIN -> true;
            case MANAGER -> targetRole == MANAGER || targetRole == MEMBER;
            default -> false;
        };
    }
}
