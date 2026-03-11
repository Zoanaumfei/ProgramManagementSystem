package com.oryzem.programmanagementsystem.operations;

import com.oryzem.programmanagementsystem.authorization.TenantType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateOperationRequest(
        @NotBlank String title,
        String description,
        @NotBlank String tenantId,
        @NotNull TenantType tenantType) {
}
