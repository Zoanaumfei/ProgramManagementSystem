package com.oryzem.programmanagementsystem.platform.users.api;

import com.oryzem.programmanagementsystem.platform.authorization.Role;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateUserRequest(
        @NotBlank String displayName,
        @Email @NotBlank String email,
        @NotNull Role role,
        @JsonAlias("tenantId") @NotBlank String organizationId) {
}

