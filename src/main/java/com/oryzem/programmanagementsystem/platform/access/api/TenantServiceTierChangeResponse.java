package com.oryzem.programmanagementsystem.platform.access.api;

import com.oryzem.programmanagementsystem.platform.access.TenantServiceTier;
import java.time.Instant;

public record TenantServiceTierChangeResponse(
        String tenantId,
        String tenantName,
        TenantServiceTier previousServiceTier,
        TenantServiceTier serviceTier,
        Instant updatedAt) {
}
