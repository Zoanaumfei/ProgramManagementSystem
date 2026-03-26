package com.oryzem.programmanagementsystem.platform.users.api;

import com.oryzem.programmanagementsystem.platform.users.domain.ManagedUser;
import com.oryzem.programmanagementsystem.platform.users.domain.UserStatus;
import java.time.Instant;

public record UserSummaryResponse(
        String id,
        String displayName,
        String email,
        UserStatus status,
        boolean membershipAssigned,
        Instant createdAt,
        Instant inviteResentAt,
        Instant accessResetAt) {

    public static UserSummaryResponse from(ManagedUser user, boolean membershipAssigned) {
        return new UserSummaryResponse(
                user.id(),
                user.displayName(),
                user.email(),
                user.status(),
                membershipAssigned,
                user.createdAt(),
                user.inviteResentAt(),
                user.accessResetAt());
    }
}

