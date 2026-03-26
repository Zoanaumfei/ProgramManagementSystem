package com.oryzem.programmanagementsystem.platform.tenant;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import java.util.UUID;

final class OrganizationIds {

    private OrganizationIds() {
    }

    static String newId(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }
}

abstract class AuditableEntity {

    private String id;
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
    private String updatedBy;

    protected void initialize(String id, String actor) {
        Instant now = Instant.now();
        this.id = id;
        this.createdAt = now;
        this.updatedAt = now;
        this.createdBy = actor;
        this.updatedBy = actor;
    }

    public void touch(String actor) {
        this.updatedAt = Instant.now();
        this.updatedBy = actor;
    }

    public String getId() {
        return id;
    }

    protected void setId(String id) {
        this.id = id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    protected void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    protected void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    protected void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    protected void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
}

@MappedSuperclass
abstract class JpaAuditableEntity extends AuditableEntity {

    @Id
    @Column(length = 64, nullable = false)
    @Override
    public String getId() {
        return super.getId();
    }

    @Column(name = "created_at", nullable = false)
    @Override
    public Instant getCreatedAt() {
        return super.getCreatedAt();
    }

    @Column(name = "updated_at", nullable = false)
    @Override
    public Instant getUpdatedAt() {
        return super.getUpdatedAt();
    }

    @Column(name = "created_by", nullable = false, length = 128)
    @Override
    public String getCreatedBy() {
        return super.getCreatedBy();
    }

    @Column(name = "updated_by", nullable = false, length = 128)
    @Override
    public String getUpdatedBy() {
        return super.getUpdatedBy();
    }
}
