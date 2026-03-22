package com.oryzem.programmanagementsystem.platform.users.api;

import com.oryzem.programmanagementsystem.platform.authorization.Role;
import com.oryzem.programmanagementsystem.platform.users.domain.ManagedUser;
import com.oryzem.programmanagementsystem.platform.users.domain.UserStatus;
import java.time.Instant;

public record UserSummaryResponse(
        String id,
        String displayName,
        String email,
        Role role,
        String organizationId,
        String organizationName,
        UserStatus status,
        Instant createdAt,
        Instant inviteResentAt,
        Instant accessResetAt) {

    public static UserSummaryResponse from(ManagedUser user, String organizationName) {
        return new UserSummaryResponse(
                user.id(),
                user.displayName(),
                user.email(),
                user.role(),
                user.tenantId(),
                organizationName,
                user.status(),
                user.createdAt(),
                user.inviteResentAt(),
                user.accessResetAt());
    }
}

