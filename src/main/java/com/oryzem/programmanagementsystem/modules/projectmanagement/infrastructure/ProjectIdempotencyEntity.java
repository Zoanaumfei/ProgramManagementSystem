package com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "project_idempotency")
@IdClass(ProjectIdempotencyEntity.ProjectIdempotencyId.class)
public class ProjectIdempotencyEntity {
    @Id
    @Column(name = "idempotency_key", length = 128, nullable = false)
    private String idempotencyKey;
    @Id
    @Column(name = "tenant_id", length = 64, nullable = false)
    private String tenantId;
    @Id
    @Column(length = 64, nullable = false)
    private String operation;
    @Column(name = "request_hash", length = 64, nullable = false)
    private String requestHash;
    @Column(name = "response_payload", nullable = false, columnDefinition = "TEXT")
    private String responsePayload;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ProjectIdempotencyEntity() {}

    public static ProjectIdempotencyEntity create(String idempotencyKey, String tenantId, String operation, String requestHash, String responsePayload, Instant createdAt) {
        ProjectIdempotencyEntity entity = new ProjectIdempotencyEntity();
        entity.idempotencyKey = idempotencyKey;
        entity.tenantId = tenantId;
        entity.operation = operation;
        entity.requestHash = requestHash;
        entity.responsePayload = responsePayload;
        entity.createdAt = createdAt;
        return entity;
    }

    public String getIdempotencyKey() { return idempotencyKey; }
    public String getTenantId() { return tenantId; }
    public String getOperation() { return operation; }
    public String getRequestHash() { return requestHash; }
    public String getResponsePayload() { return responsePayload; }
    public Instant getCreatedAt() { return createdAt; }

    public static class ProjectIdempotencyId implements Serializable {
        private String idempotencyKey;
        private String tenantId;
        private String operation;
        public ProjectIdempotencyId() {}
        public ProjectIdempotencyId(String idempotencyKey, String tenantId, String operation) {
            this.idempotencyKey = idempotencyKey;
            this.tenantId = tenantId;
            this.operation = operation;
        }
        @Override
        public boolean equals(Object other) {
            if (this == other) { return true; }
            if (!(other instanceof ProjectIdempotencyId that)) { return false; }
            return Objects.equals(idempotencyKey, that.idempotencyKey) && Objects.equals(tenantId, that.tenantId) && Objects.equals(operation, that.operation);
        }
        @Override
        public int hashCode() {
            return Objects.hash(idempotencyKey, tenantId, operation);
        }
    }
}
