package com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "project_structure_template")
public class ProjectStructureTemplateEntity {
    @Id
    @Column(length = 64, nullable = false)
    private String id;
    @Column(length = 160, nullable = false)
    private String name;
    @Column(name = "framework_type", length = 32, nullable = false)
    private String frameworkType;
    @Column(nullable = false)
    private int version;
    @Column(nullable = false)
    private boolean active;
    @Column(name = "owner_organization_id", length = 64, nullable = false)
    private String ownerOrganizationId;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ProjectStructureTemplateEntity() {}

    public static ProjectStructureTemplateEntity create(
            String id,
            String name,
            String frameworkType,
            int version,
            boolean active,
            String ownerOrganizationId,
            Instant createdAt) {
        ProjectStructureTemplateEntity entity = new ProjectStructureTemplateEntity();
        entity.id = id;
        entity.name = name;
        entity.frameworkType = frameworkType;
        entity.version = version;
        entity.active = active;
        entity.ownerOrganizationId = ownerOrganizationId;
        entity.createdAt = createdAt;
        return entity;
    }

    public void rename(String name) {
        this.name = name;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getFrameworkType() { return frameworkType; }
    public int getVersion() { return version; }
    public boolean isActive() { return active; }
    public String getOwnerOrganizationId() { return ownerOrganizationId; }
    public Instant getCreatedAt() { return createdAt; }
}
