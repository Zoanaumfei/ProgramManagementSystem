package com.oryzem.programmanagementsystem.platform.access;

import com.oryzem.programmanagementsystem.platform.authorization.Role;
import com.oryzem.programmanagementsystem.platform.authorization.TenantType;
import java.util.Set;

public record ResolvedMembershipContext(
        String userId,
        String membershipId,
        String activeTenantId,
        String activeOrganizationId,
        String activeMarketId,
        TenantType tenantType,
        Set<Role> roles,
        Set<String> permissions) {
}
