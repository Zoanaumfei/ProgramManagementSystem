package com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure;

import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectMilestoneAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectMilestoneStatus;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectVisibilityScope;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDate;

@Entity
@Table(name = "project_milestone")
public class ProjectMilestoneEntity {
    @Id
    @Column(length = 64, nullable = false)
    private String id;
    @Column(name = "project_id", length = 64, nullable = false)
    private String projectId;
    @Column(name = "structure_node_id", length = 64, nullable = false)
    private String structureNodeId;
    @Column(name = "phase_id", length = 64)
    private String phaseId;
    @Column(length = 64, nullable = false)
    private String code;
    @Column(length = 160, nullable = false)
    private String name;
    @Column(name = "sequence_no", nullable = false)
    private int sequenceNo;
    @Column(name = "planned_date")
    private LocalDate plannedDate;
    @Column(name = "actual_date")
    private LocalDate actualDate;
    @Enumerated(EnumType.STRING)
    @Column(length = 32, nullable = false)
    private ProjectMilestoneStatus status;
    @Column(name = "owner_organization_id", length = 64)
    private String ownerOrganizationId;
    @Enumerated(EnumType.STRING)
    @Column(name = "visibility_scope", length = 32, nullable = false)
    private ProjectVisibilityScope visibilityScope;
    @Version
    @Column(nullable = false)
    private long version;

    protected ProjectMilestoneEntity() {}

    public static ProjectMilestoneEntity create(ProjectMilestoneAggregate aggregate) {
        ProjectMilestoneEntity entity = new ProjectMilestoneEntity();
        entity.id = aggregate.id();
        entity.projectId = aggregate.projectId();
        entity.structureNodeId = aggregate.structureNodeId();
        entity.phaseId = aggregate.phaseId();
        entity.code = aggregate.code();
        entity.name = aggregate.name();
        entity.sequenceNo = aggregate.sequence();
        entity.plannedDate = aggregate.plannedDate();
        entity.actualDate = aggregate.actualDate();
        entity.status = aggregate.status();
        entity.ownerOrganizationId = aggregate.ownerOrganizationId();
        entity.visibilityScope = aggregate.visibilityScope();
        entity.version = aggregate.version();
        return entity;
    }

    public ProjectMilestoneAggregate toDomain() {
        return new ProjectMilestoneAggregate(id, projectId, structureNodeId, phaseId, code, name, sequenceNo, plannedDate, actualDate, status, ownerOrganizationId, visibilityScope, version);
    }

    public void apply(ProjectMilestoneAggregate aggregate) {
        this.plannedDate = aggregate.plannedDate();
        this.actualDate = aggregate.actualDate();
        this.status = aggregate.status();
        this.ownerOrganizationId = aggregate.ownerOrganizationId();
        this.visibilityScope = aggregate.visibilityScope();
    }

    public String getId() { return id; }
    public String getProjectId() { return projectId; }
    public String getStructureNodeId() { return structureNodeId; }
    public String getPhaseId() { return phaseId; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public int getSequenceNo() { return sequenceNo; }
    public LocalDate getPlannedDate() { return plannedDate; }
    public LocalDate getActualDate() { return actualDate; }
    public ProjectMilestoneStatus getStatus() { return status; }
    public String getOwnerOrganizationId() { return ownerOrganizationId; }
    public ProjectVisibilityScope getVisibilityScope() { return visibilityScope; }
    public long getVersion() { return version; }
}
