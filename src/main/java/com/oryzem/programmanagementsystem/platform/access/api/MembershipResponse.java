package com.oryzem.programmanagementsystem.platform.access.api;

import com.oryzem.programmanagementsystem.platform.access.MembershipStatus;
import java.time.Instant;
import java.util.List;

public record MembershipResponse(
        String id,
        String userId,
        String tenantId,
        String tenantName,
        String organizationId,
        String organizationName,
        String marketId,
        String marketName,
        MembershipStatus status,
        boolean defaultMembership,
        Instant joinedAt,
        Instant updatedAt,
        List<String> roles,
        List<String> permissions) {
}
