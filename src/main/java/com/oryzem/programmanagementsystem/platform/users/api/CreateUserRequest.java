package com.oryzem.programmanagementsystem.platform.users.api;

import com.oryzem.programmanagementsystem.platform.authorization.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.Set;

public record CreateUserRequest(
        @NotBlank String displayName,
        @Email @NotBlank String email,
        @NotBlank String organizationId,
        String marketId,
        @NotEmpty Set<Role> roles) {
}

