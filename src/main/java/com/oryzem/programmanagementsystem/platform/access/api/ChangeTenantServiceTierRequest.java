package com.oryzem.programmanagementsystem.platform.access.api;

import com.oryzem.programmanagementsystem.platform.access.TenantServiceTier;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ChangeTenantServiceTierRequest(
        @NotNull TenantServiceTier serviceTier,
        @NotBlank String justification) {
}
