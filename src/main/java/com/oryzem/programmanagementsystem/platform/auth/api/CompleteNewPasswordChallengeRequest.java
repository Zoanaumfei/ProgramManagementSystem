package com.oryzem.programmanagementsystem.platform.auth.api;

import jakarta.validation.constraints.NotBlank;

public record CompleteNewPasswordChallengeRequest(
        @NotBlank String username,
        @NotBlank String session,
        @NotBlank String newPassword) {
}
