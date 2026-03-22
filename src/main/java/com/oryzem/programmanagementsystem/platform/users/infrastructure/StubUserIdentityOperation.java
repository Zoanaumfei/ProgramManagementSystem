package com.oryzem.programmanagementsystem.platform.users.infrastructure;

import com.oryzem.programmanagementsystem.platform.authorization.Role;
import com.oryzem.programmanagementsystem.platform.authorization.TenantType;

public record StubUserIdentityOperation(
        String action,
        String identityUsername,
        String email,
        Role role,
        String tenantId,
        TenantType tenantType) {
}
