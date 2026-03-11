package com.oryzem.programmanagementsystem.users;

import com.oryzem.programmanagementsystem.authorization.Role;
import com.oryzem.programmanagementsystem.authorization.TenantType;
import java.time.Instant;

public record ManagedUser(
        String id,
        String displayName,
        String email,
        Role role,
        String tenantId,
        TenantType tenantType,
        UserStatus status,
        Instant createdAt,
        Instant inviteResentAt,
        Instant accessResetAt) {

    public ManagedUser withInviteResentAt(Instant performedAt) {
        return new ManagedUser(id, displayName, email, role, tenantId, tenantType, status, createdAt, performedAt, accessResetAt);
    }

    public ManagedUser withAccessResetAt(Instant performedAt) {
        return new ManagedUser(id, displayName, email, role, tenantId, tenantType, status, createdAt, inviteResentAt, performedAt);
    }
}
