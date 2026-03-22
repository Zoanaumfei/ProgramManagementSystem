package com.oryzem.programmanagementsystem.modules.operations;

import com.oryzem.programmanagementsystem.platform.authorization.TenantType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateOperationRequest(
        @NotBlank String title,
        String description,
        @NotBlank String tenantId,
        @NotNull TenantType tenantType) {
}

