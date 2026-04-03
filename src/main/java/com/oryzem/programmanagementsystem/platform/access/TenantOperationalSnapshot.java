package com.oryzem.programmanagementsystem.platform.access;

public record TenantOperationalSnapshot(
        String tenantId,
        String tenantName,
        String tenantCode,
        String tenantStatus,
        String tenantTier,
        String tenantType,
        String rootOrganizationId,
        String dataRegion,
        long marketCount,
        long activeMarketCount,
        long membershipCount,
        long activeMembershipCount,
        long userCount,
        long activeUserCount) {
}
