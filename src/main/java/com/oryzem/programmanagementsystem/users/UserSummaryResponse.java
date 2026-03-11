package com.oryzem.programmanagementsystem.users;

import com.oryzem.programmanagementsystem.authorization.Role;
import com.oryzem.programmanagementsystem.authorization.TenantType;
import java.time.Instant;

public record UserSummaryResponse(
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

    public static UserSummaryResponse from(ManagedUser user) {
        return new UserSummaryResponse(
                user.id(),
                user.displayName(),
                user.email(),
                user.role(),
                user.tenantId(),
                user.tenantType(),
                user.status(),
                user.createdAt(),
                user.inviteResentAt(),
                user.accessResetAt());
    }
}
