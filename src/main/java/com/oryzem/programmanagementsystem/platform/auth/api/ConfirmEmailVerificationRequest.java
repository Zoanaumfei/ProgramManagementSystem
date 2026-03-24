package com.oryzem.programmanagementsystem.platform.auth.api;

import jakarta.validation.constraints.NotBlank;

public record ConfirmEmailVerificationRequest(
        @NotBlank(message = "code is required") String code) {
}
