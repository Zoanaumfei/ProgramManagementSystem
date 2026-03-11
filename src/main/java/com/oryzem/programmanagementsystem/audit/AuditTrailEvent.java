package com.oryzem.programmanagementsystem.audit;

import com.oryzem.programmanagementsystem.authorization.Role;
import java.time.Instant;

public record AuditTrailEvent(
        String id,
        String eventType,
        String actorUserId,
        Role actorRole,
        String actorTenantId,
        String targetTenantId,
        String targetResourceType,
        String targetResourceId,
        String justification,
        String metadataJson,
        boolean crossTenant,
        String correlationId,
        String sourceModule,
        Instant createdAt) {
}
