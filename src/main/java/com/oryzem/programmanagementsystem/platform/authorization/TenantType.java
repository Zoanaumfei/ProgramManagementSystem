package com.oryzem.programmanagementsystem.platform.authorization;

import java.util.Locale;
import java.util.Optional;

public enum TenantType {
    INTERNAL,
    EXTERNAL;

    public static Optional<TenantType> fromClaim(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }

        try {
            return Optional.of(TenantType.valueOf(value.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
