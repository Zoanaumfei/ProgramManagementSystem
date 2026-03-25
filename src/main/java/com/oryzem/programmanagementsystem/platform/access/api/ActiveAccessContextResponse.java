package com.oryzem.programmanagementsystem.platform.access.api;

import com.oryzem.programmanagementsystem.platform.authorization.TenantType;
import java.util.List;

public record ActiveAccessContextResponse(
        String userId,
        String membershipId,
        String activeTenantId,
        String activeOrganizationId,
        String activeMarketId,
        TenantType tenantType,
        List<String> roles,
        List<String> permissions) {
}
