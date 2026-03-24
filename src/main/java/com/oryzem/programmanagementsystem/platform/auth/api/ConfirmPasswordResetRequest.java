package com.oryzem.programmanagementsystem.platform.auth.api;

import jakarta.validation.constraints.NotBlank;

public record ConfirmPasswordResetRequest(
        @NotBlank String username,
        @NotBlank String code,
        @NotBlank String newPassword) {
}
