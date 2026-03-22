package com.oryzem.programmanagementsystem.modules.operations;

import com.oryzem.programmanagementsystem.platform.authorization.TenantType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "operation_record")
public class OperationEntity {

    @Id
    @Column(length = 64, nullable = false)
    private String id;

    @Column(length = 255, nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "tenant_id", length = 64, nullable = false)
    private String tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tenant_type", length = 32, nullable = false)
    private TenantType tenantType;

    @Column(name = "created_by", length = 64, nullable = false)
    private String createdBy;

    @Enumerated(EnumType.STRING)
    @Column(length = 32, nullable = false)
    private OperationStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "rejected_at")
    private Instant rejectedAt;

    @Column(name = "reopened_at")
    private Instant reopenedAt;

    @Column(name = "reprocessed_at")
    private Instant reprocessedAt;

    protected OperationEntity() {
    }

    private OperationEntity(
            String id,
            String title,
            String description,
            String tenantId,
            TenantType tenantType,
            String createdBy,
            OperationStatus status,
            Instant createdAt,
            Instant updatedAt,
            Instant submittedAt,
            Instant approvedAt,
            Instant rejectedAt,
            Instant reopenedAt,
            Instant reprocessedAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.tenantId = tenantId;
        this.tenantType = tenantType;
        this.createdBy = createdBy;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.submittedAt = submittedAt;
        this.approvedAt = approvedAt;
        this.rejectedAt = rejectedAt;
        this.reopenedAt = reopenedAt;
        this.reprocessedAt = reprocessedAt;
    }

    public static OperationEntity fromDomain(OperationRecord record) {
        return new OperationEntity(
                record.id(),
                record.title(),
                record.description(),
                record.tenantId(),
                record.tenantType(),
                record.createdBy(),
                record.status(),
                record.createdAt(),
                record.updatedAt(),
                record.submittedAt(),
                record.approvedAt(),
                record.rejectedAt(),
                record.reopenedAt(),
                record.reprocessedAt());
    }

    public OperationRecord toDomain() {
        return new OperationRecord(
                id,
                title,
                description,
                tenantId,
                tenantType,
                createdBy,
                status,
                createdAt,
                updatedAt,
                submittedAt,
                approvedAt,
                rejectedAt,
                reopenedAt,
                reprocessedAt);
    }
}

