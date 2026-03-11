package com.oryzem.programmanagementsystem.audit;

import com.oryzem.programmanagementsystem.authorization.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "audit_log")
public class AuditLogEntity {

    @Id
    @Column(name = "id", nullable = false, length = 64)
    private String id;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "actor_user_id", nullable = false, length = 64)
    private String actorUserId;

    @Column(name = "actor_role", nullable = false, length = 32)
    private String actorRole;

    @Column(name = "actor_tenant_id", length = 64)
    private String actorTenantId;

    @Column(name = "target_tenant_id", length = 64)
    private String targetTenantId;

    @Column(name = "target_resource_type", nullable = false, length = 64)
    private String targetResourceType;

    @Column(name = "target_resource_id", length = 64)
    private String targetResourceId;

    @Column(name = "justification", columnDefinition = "TEXT")
    private String justification;

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;

    @Column(name = "cross_tenant", nullable = false)
    private boolean crossTenant;

    @Column(name = "correlation_id", nullable = false, length = 64)
    private String correlationId;

    @Column(name = "source_module", nullable = false, length = 32)
    private String sourceModule;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AuditLogEntity() {
    }

    private AuditLogEntity(
            String id,
            String eventType,
            String actorUserId,
            String actorRole,
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
        this.id = id;
        this.eventType = eventType;
        this.actorUserId = actorUserId;
        this.actorRole = actorRole;
        this.actorTenantId = actorTenantId;
        this.targetTenantId = targetTenantId;
        this.targetResourceType = targetResourceType;
        this.targetResourceId = targetResourceId;
        this.justification = justification;
        this.metadataJson = metadataJson;
        this.crossTenant = crossTenant;
        this.correlationId = correlationId;
        this.sourceModule = sourceModule;
        this.createdAt = createdAt;
    }

    public static AuditLogEntity from(AuditTrailEvent event) {
        return new AuditLogEntity(
                event.id(),
                event.eventType(),
                event.actorUserId(),
                event.actorRole().name(),
                event.actorTenantId(),
                event.targetTenantId(),
                event.targetResourceType(),
                event.targetResourceId(),
                event.justification(),
                event.metadataJson(),
                event.crossTenant(),
                event.correlationId(),
                event.sourceModule(),
                event.createdAt());
    }

    public AuditTrailEvent toDomain() {
        return new AuditTrailEvent(
                id,
                eventType,
                actorUserId,
                Role.valueOf(actorRole),
                actorTenantId,
                targetTenantId,
                targetResourceType,
                targetResourceId,
                justification,
                metadataJson,
                crossTenant,
                correlationId,
                sourceModule,
                createdAt);
    }
}
