package com.oryzem.programmanagementsystem.platform.access.api;

public record TenantSummaryResponse(
        String id,
        String name,
        String code,
        String status,
        String tenantType,
        String rootOrganizationId) {
}
