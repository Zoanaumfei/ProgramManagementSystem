package com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure;

import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectTemplateStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "project_template")
public class ProjectTemplateEntity {
    @Id
    @Column(length = 64, nullable = false)
    private String id;
    @Column(length = 160, nullable = false)
    private String name;
    @Column(name = "framework_type", length = 32, nullable = false)
    private String frameworkType;
    @Column(nullable = false)
    private int version;
    @Enumerated(EnumType.STRING)
    @Column(length = 32, nullable = false)
    private ProjectTemplateStatus status;
    @Column(name = "structure_template_id", length = 64, nullable = false)
    private String structureTemplateId;
    @Column(name = "owner_organization_id", length = 64, nullable = false)
    private String ownerOrganizationId;
    @Column(name = "is_default", nullable = false)
    private boolean isDefault;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ProjectTemplateEntity() {}

    public static ProjectTemplateEntity create(
            String id,
            String name,
            String frameworkType,
            int version,
            ProjectTemplateStatus status,
            String structureTemplateId,
            String ownerOrganizationId,
            boolean isDefault,
            Instant createdAt) {
        ProjectTemplateEntity entity = new ProjectTemplateEntity();
        entity.id = id;
        entity.name = name;
        entity.frameworkType = frameworkType;
        entity.version = version;
        entity.status = status;
        entity.structureTemplateId = structureTemplateId;
        entity.ownerOrganizationId = ownerOrganizationId;
        entity.isDefault = isDefault;
        entity.createdAt = createdAt;
        return entity;
    }

    public void applyUpdate(String name, ProjectTemplateStatus status, String structureTemplateId, boolean isDefault) {
        this.name = name;
        this.status = status;
        this.structureTemplateId = structureTemplateId;
        this.isDefault = isDefault;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getFrameworkType() { return frameworkType; }
    public int getVersion() { return version; }
    public ProjectTemplateStatus getStatus() { return status; }
    public String getStructureTemplateId() { return structureTemplateId; }
    public String getOwnerOrganizationId() { return ownerOrganizationId; }
    public boolean isDefault() { return isDefault; }
    public Instant getCreatedAt() { return createdAt; }
}
