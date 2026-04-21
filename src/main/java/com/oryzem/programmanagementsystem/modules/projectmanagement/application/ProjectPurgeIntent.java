package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import java.time.Instant;

public record ProjectPurgeIntent(
        String token,
        String projectId,
        String requestedByUserId,
        String requestedByUsername,
        String reason,
        ProjectPurgeIntentStatus status,
        Instant createdAt,
        Instant expiresAt,
        Instant consumedAt) {

    public boolean isExpired(Instant now) {
        return now != null && expiresAt != null && now.isAfter(expiresAt);
    }

    public ProjectPurgeIntent markConsumed(Instant now) {
        return new ProjectPurgeIntent(
                token,
                projectId,
                requestedByUserId,
                requestedByUsername,
                reason,
                ProjectPurgeIntentStatus.CONSUMED,
                createdAt,
                expiresAt,
                now);
    }

    public ProjectPurgeIntent markExpired() {
        return new ProjectPurgeIntent(
                token,
                projectId,
                requestedByUserId,
                requestedByUsername,
                reason,
                ProjectPurgeIntentStatus.EXPIRED,
                createdAt,
                expiresAt,
                consumedAt);
    }
}
