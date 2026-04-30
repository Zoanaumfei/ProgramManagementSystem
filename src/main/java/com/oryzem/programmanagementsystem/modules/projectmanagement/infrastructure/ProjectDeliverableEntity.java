package com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure;

import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.DeliverableType;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectDeliverableAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectDeliverableStatus;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectPriority;
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
@Table(name = "project_deliverable")
public class ProjectDeliverableEntity {
    @Id
    @Column(length = 64, nullable = false)
    private String id;
    @Column(name = "project_id", length = 64, nullable = false)
    private String projectId;
    @Column(name = "structure_node_id", length = 64, nullable = false)
    private String structureNodeId;
    @Column(name = "phase_id", length = 64)
    private String phaseId;
    @Column(name = "milestone_id", length = 64)
    private String milestoneId;
    @Column(length = 64, nullable = false)
    private String code;
    @Column(length = 160, nullable = false)
    private String name;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Enumerated(EnumType.STRING)
    @Column(name = "deliverable_type", length = 32, nullable = false)
    private DeliverableType deliverableType;
    @Column(name = "responsible_organization_id", length = 64)
    private String responsibleOrganizationId;
    @Column(name = "responsible_user_id", length = 64)
    private String responsibleUserId;
    @Column(name = "approver_organization_id", length = 64)
    private String approverOrganizationId;
    @Column(name = "approver_user_id", length = 64)
    private String approverUserId;
    @Column(name = "required_document", nullable = false)
    private boolean requiredDocument;
    @Column(name = "planned_due_date")
    private LocalDate plannedDueDate;
    @Column(name = "submitted_at")
    private Instant submittedAt;
    @Column(name = "approved_at")
    private Instant approvedAt;
    @Enumerated(EnumType.STRING)
    @Column(length = 32, nullable = false)
    private ProjectDeliverableStatus status;
    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private ProjectPriority priority;
    @Enumerated(EnumType.STRING)
    @Column(name = "visibility_scope", length = 32, nullable = false)
    private ProjectVisibilityScope visibilityScope;
    @Version
    @Column(nullable = false)
    private long version;

    protected ProjectDeliverableEntity() {}

    public static ProjectDeliverableEntity create(ProjectDeliverableAggregate aggregate) {
        ProjectDeliverableEntity entity = new ProjectDeliverableEntity();
        entity.id = aggregate.id();
        entity.projectId = aggregate.projectId();
        entity.structureNodeId = aggregate.structureNodeId();
        entity.phaseId = aggregate.phaseId();
        entity.milestoneId = aggregate.milestoneId();
        entity.code = aggregate.code();
        entity.name = aggregate.name();
        entity.description = aggregate.description();
        entity.deliverableType = aggregate.deliverableType();
        entity.responsibleOrganizationId = aggregate.responsibleOrganizationId();
        entity.responsibleUserId = aggregate.responsibleUserId();
        entity.approverOrganizationId = aggregate.approverOrganizationId();
        entity.approverUserId = aggregate.approverUserId();
        entity.requiredDocument = aggregate.requiredDocument();
        entity.plannedDueDate = aggregate.plannedDueDate();
        entity.submittedAt = aggregate.submittedAt();
        entity.approvedAt = aggregate.approvedAt();
        entity.status = aggregate.status();
        entity.priority = aggregate.priority();
        entity.visibilityScope = aggregate.visibilityScope();
        entity.version = aggregate.version();
        return entity;
    }

    public ProjectDeliverableAggregate toDomain() {
        return new ProjectDeliverableAggregate(id, projectId, structureNodeId, phaseId, milestoneId, code, name, description, deliverableType, responsibleOrganizationId, responsibleUserId, approverOrganizationId, approverUserId, requiredDocument, plannedDueDate, submittedAt, approvedAt, status, priority, visibilityScope, version);
    }

    public void apply(ProjectDeliverableAggregate aggregate) {
        this.code = aggregate.code();
        this.name = aggregate.name();
        this.description = aggregate.description();
        this.deliverableType = aggregate.deliverableType();
        this.responsibleOrganizationId = aggregate.responsibleOrganizationId();
        this.responsibleUserId = aggregate.responsibleUserId();
        this.approverOrganizationId = aggregate.approverOrganizationId();
        this.approverUserId = aggregate.approverUserId();
        this.requiredDocument = aggregate.requiredDocument();
        this.plannedDueDate = aggregate.plannedDueDate();
        this.submittedAt = aggregate.submittedAt();
        this.approvedAt = aggregate.approvedAt();
        this.status = aggregate.status();
        this.priority = aggregate.priority();
        this.visibilityScope = aggregate.visibilityScope();
    }

    public String getId() { return id; }
    public String getProjectId() { return projectId; }
    public String getStructureNodeId() { return structureNodeId; }
    public String getPhaseId() { return phaseId; }
    public String getMilestoneId() { return milestoneId; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public DeliverableType getDeliverableType() { return deliverableType; }
    public String getResponsibleOrganizationId() { return responsibleOrganizationId; }
    public String getResponsibleUserId() { return responsibleUserId; }
    public String getApproverOrganizationId() { return approverOrganizationId; }
    public String getApproverUserId() { return approverUserId; }
    public boolean isRequiredDocument() { return requiredDocument; }
    public LocalDate getPlannedDueDate() { return plannedDueDate; }
    public Instant getSubmittedAt() { return submittedAt; }
    public Instant getApprovedAt() { return approvedAt; }
    public ProjectDeliverableStatus getStatus() { return status; }
    public ProjectPriority getPriority() { return priority; }
    public ProjectVisibilityScope getVisibilityScope() { return visibilityScope; }
    public long getVersion() { return version; }
}
