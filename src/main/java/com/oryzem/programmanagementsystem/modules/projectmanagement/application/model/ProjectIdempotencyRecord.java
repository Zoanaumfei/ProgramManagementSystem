package com.oryzem.programmanagementsystem.modules.projectmanagement.application.model;

import java.time.Instant;

public record ProjectIdempotencyRecord(
        String idempotencyKey,
        String tenantId,
        String operation,
        String requestHash,
        String responsePayload,
        Instant createdAt) {
}
