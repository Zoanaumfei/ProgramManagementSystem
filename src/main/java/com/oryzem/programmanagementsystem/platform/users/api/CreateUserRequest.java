package com.oryzem.programmanagementsystem.platform.users.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateUserRequest(
        @NotBlank String displayName,
        @Email @NotBlank String email) {
}

