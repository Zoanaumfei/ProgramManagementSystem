package com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure;

import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectStatus;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectVisibilityScope;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "project")
public class ProjectEntity {

    @Id
    @Column(length = 64, nullable = false)
    private String id;
    @Column(name = "tenant_id", length = 64, nullable = false)
    private String tenantId;
    @Column(length = 64, nullable = false)
    private String code;
    @Column(length = 160, nullable = false)
    private String name;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Column(name = "framework_type", length = 32, nullable = false)
    private String frameworkType;
    @Column(name = "template_id", length = 64, nullable = false)
    private String templateId;
    @Column(name = "template_version", nullable = false)
    private int templateVersion;
    @Column(name = "lead_organization_id", length = 64, nullable = false)
    private String leadOrganizationId;
    @Column(name = "customer_organization_id", length = 64)
    private String customerOrganizationId;
    @Enumerated(EnumType.STRING)
    @Column(length = 32, nullable = false)
    private ProjectStatus status;
    @Enumerated(EnumType.STRING)
    @Column(name = "visibility_scope", length = 32, nullable = false)
    private ProjectVisibilityScope visibilityScope;
    @Column(name = "planned_start_date")
    private LocalDate plannedStartDate;
    @Column(name = "planned_end_date")
    private LocalDate plannedEndDate;
    @Column(name = "actual_start_date")
    private LocalDate actualStartDate;
    @Column(name = "actual_end_date")
    private LocalDate actualEndDate;
    @Column(name = "created_by_user_id", length = 64, nullable = false)
    private String createdByUserId;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Version
    @Column(nullable = false)
    private long version;

    protected ProjectEntity() {}

    public static ProjectEntity create(ProjectAggregate aggregate) {
        ProjectEntity entity = new ProjectEntity();
        entity.id = aggregate.id();
        entity.tenantId = aggregate.tenantId();
        entity.code = aggregate.code();
        entity.name = aggregate.name();
        entity.description = aggregate.description();
        entity.frameworkType = aggregate.frameworkType();
        entity.templateId = aggregate.templateId();
        entity.templateVersion = aggregate.templateVersion();
        entity.leadOrganizationId = aggregate.leadOrganizationId();
        entity.customerOrganizationId = aggregate.customerOrganizationId();
        entity.status = aggregate.status();
        entity.visibilityScope = aggregate.visibilityScope();
        entity.plannedStartDate = aggregate.plannedStartDate();
        entity.plannedEndDate = aggregate.plannedEndDate();
        entity.actualStartDate = aggregate.actualStartDate();
        entity.actualEndDate = aggregate.actualEndDate();
        entity.createdByUserId = aggregate.createdByUserId();
        entity.createdAt = aggregate.createdAt();
        entity.updatedAt = aggregate.updatedAt();
        entity.version = aggregate.version();
        return entity;
    }

    public ProjectAggregate toDomain() {
        return new ProjectAggregate(id, tenantId, code, name, description, frameworkType, templateId, templateVersion, leadOrganizationId, customerOrganizationId, status, visibilityScope, plannedStartDate, plannedEndDate, actualStartDate, actualEndDate, createdByUserId, createdAt, updatedAt, version);
    }

    public void apply(ProjectAggregate aggregate) {
        this.name = aggregate.name();
        this.description = aggregate.description();
        this.status = aggregate.status();
        this.visibilityScope = aggregate.visibilityScope();
        this.plannedStartDate = aggregate.plannedStartDate();
        this.plannedEndDate = aggregate.plannedEndDate();
        this.actualStartDate = aggregate.actualStartDate();
        this.actualEndDate = aggregate.actualEndDate();
        this.updatedAt = aggregate.updatedAt();
    }

    public String getId() { return id; }
    public String getTenantId() { return tenantId; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getFrameworkType() { return frameworkType; }
    public String getTemplateId() { return templateId; }
    public int getTemplateVersion() { return templateVersion; }
    public String getLeadOrganizationId() { return leadOrganizationId; }
    public String getCustomerOrganizationId() { return customerOrganizationId; }
    public ProjectStatus getStatus() { return status; }
    public ProjectVisibilityScope getVisibilityScope() { return visibilityScope; }
    public LocalDate getPlannedStartDate() { return plannedStartDate; }
    public LocalDate getPlannedEndDate() { return plannedEndDate; }
    public LocalDate getActualStartDate() { return actualStartDate; }
    public LocalDate getActualEndDate() { return actualEndDate; }
    public String getCreatedByUserId() { return createdByUserId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
