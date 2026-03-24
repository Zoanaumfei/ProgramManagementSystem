package com.oryzem.programmanagementsystem.platform.auth;

import org.springframework.util.StringUtils;

public record CurrentUserEmailVerificationState(
        String email,
        boolean emailVerified) {

    public boolean emailVerificationRequired() {
        return StringUtils.hasText(email) && !emailVerified;
    }
}
