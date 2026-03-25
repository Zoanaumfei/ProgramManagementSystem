package com.oryzem.programmanagementsystem.platform.authorization;

import java.util.Set;

public record AuthenticatedUser(
        String subject,
        String username,
        Set<Role> roles,
        Set<String> permissions,
        String userId,
        String activeMembershipId,
        String activeTenantId,
        String activeOrganizationId,
        String activeMarketId,
        TenantType tenantType) {

    public AuthenticatedUser(String subject, String username, Set<Role> roles, String tenantId, TenantType tenantType) {
        this(subject, username, roles, Set.of(), null, null, tenantId, tenantId, null, tenantType);
    }

    public boolean hasRole(Role role) {
        return roles != null && roles.contains(role);
    }

    public boolean hasPermission(String permissionCode) {
        return permissions != null && permissions.contains(permissionCode);
    }

    public boolean isAdmin() {
        return hasRole(Role.ADMIN);
    }

    public String tenantId() {
        return activeTenantId;
    }

    public String organizationId() {
        return activeOrganizationId;
    }

    public String marketId() {
        return activeMarketId;
    }

    public String membershipId() {
        return activeMembershipId;
    }
}
