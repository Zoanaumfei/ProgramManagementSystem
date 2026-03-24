package com.oryzem.programmanagementsystem.platform.auth.api;

import jakarta.validation.constraints.NotBlank;

public record RefreshSessionRequest(
        @NotBlank String username,
        @NotBlank String refreshToken) {
}
