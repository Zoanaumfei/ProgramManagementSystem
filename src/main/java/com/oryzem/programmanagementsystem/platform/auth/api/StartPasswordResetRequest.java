package com.oryzem.programmanagementsystem.platform.auth.api;

import jakarta.validation.constraints.NotBlank;

public record StartPasswordResetRequest(
        @NotBlank String username) {
}
