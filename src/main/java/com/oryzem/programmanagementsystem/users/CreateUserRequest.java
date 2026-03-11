package com.oryzem.programmanagementsystem.users;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.oryzem.programmanagementsystem.authorization.Role;
import com.oryzem.programmanagementsystem.authorization.TenantType;

public record CreateUserRequest(
        @NotBlank String displayName,
        @Email @NotBlank String email,
        @NotNull Role role,
        @NotBlank String tenantId,
        @NotNull TenantType tenantType) {
}
