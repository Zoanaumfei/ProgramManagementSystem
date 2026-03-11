package com.oryzem.programmanagementsystem.authorization;

import java.util.Set;

public record AuthenticatedUser(
        String subject,
        String username,
        Set<Role> roles,
        String tenantId,
        TenantType tenantType) {

    public boolean hasRole(Role role) {
        return roles != null && roles.contains(role);
    }

    public boolean isAdmin() {
        return hasRole(Role.ADMIN);
    }
}
