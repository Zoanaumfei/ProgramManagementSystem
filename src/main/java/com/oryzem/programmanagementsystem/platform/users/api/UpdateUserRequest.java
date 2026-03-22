package com.oryzem.programmanagementsystem.platform.users.api;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.oryzem.programmanagementsystem.platform.authorization.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateUserRequest(
        @NotBlank String displayName,
        @Email @NotBlank String email,
        @NotNull Role role,
        @JsonAlias("tenantId") @NotBlank String organizationId) {
}

