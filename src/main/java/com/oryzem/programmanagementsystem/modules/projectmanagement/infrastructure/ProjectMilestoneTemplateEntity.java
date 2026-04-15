package com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure;

import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectOrganizationRoleType;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectTemplateAppliesToType;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectVisibilityScope;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "project_milestone_template")
public class ProjectMilestoneTemplateEntity {
    @Id
    @Column(length = 64, nullable = false)
    private String id;
    @Column(name = "template_id", length = 64, nullable = false)
    private String templateId;
    @Column(name = "phase_template_id", length = 64)
    private String phaseTemplateId;
    @Column(length = 64, nullable = false)
    private String code;
    @Column(length = 160, nullable = false)
    private String name;
    @Column(name = "sequence_no", nullable = false)
    private int sequenceNo;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Column(name = "planned_offset_days", nullable = false)
    private int plannedOffsetDays;
    @Enumerated(EnumType.STRING)
    @Column(name = "applies_to_type", length = 32, nullable = false)
    private ProjectTemplateAppliesToType appliesToType;
    @Column(name = "structure_level_template_id", length = 64)
    private String structureLevelTemplateId;
    @Enumerated(EnumType.STRING)
    @Column(name = "owner_organization_role", length = 32)
    private ProjectOrganizationRoleType ownerOrganizationRole;
    @Enumerated(EnumType.STRING)
    @Column(name = "visibility_scope", length = 32, nullable = false)
    private ProjectVisibilityScope visibilityScope;

    protected ProjectMilestoneTemplateEntity() {}

    public static ProjectMilestoneTemplateEntity create(
            String id,
            String templateId,
            String phaseTemplateId,
            String code,
            String name,
            int sequenceNo,
            String description,
            int plannedOffsetDays,
            ProjectTemplateAppliesToType appliesToType,
            String structureLevelTemplateId,
            ProjectOrganizationRoleType ownerOrganizationRole,
            ProjectVisibilityScope visibilityScope) {
        ProjectMilestoneTemplateEntity entity = new ProjectMilestoneTemplateEntity();
        entity.id = id;
        entity.templateId = templateId;
        entity.phaseTemplateId = phaseTemplateId;
        entity.code = code;
        entity.name = name;
        entity.sequenceNo = sequenceNo;
        entity.description = description;
        entity.plannedOffsetDays = plannedOffsetDays;
        entity.appliesToType = appliesToType;
        entity.structureLevelTemplateId = structureLevelTemplateId;
        entity.ownerOrganizationRole = ownerOrganizationRole;
        entity.visibilityScope = visibilityScope;
        return entity;
    }

    public void applyUpdate(
            String phaseTemplateId,
            String code,
            String name,
            String description,
            int plannedOffsetDays,
            ProjectTemplateAppliesToType appliesToType,
            String structureLevelTemplateId,
            ProjectOrganizationRoleType ownerOrganizationRole,
            ProjectVisibilityScope visibilityScope) {
        this.phaseTemplateId = phaseTemplateId;
        this.code = code;
        this.name = name;
        this.description = description;
        this.plannedOffsetDays = plannedOffsetDays;
        this.appliesToType = appliesToType;
        this.structureLevelTemplateId = structureLevelTemplateId;
        this.ownerOrganizationRole = ownerOrganizationRole;
        this.visibilityScope = visibilityScope;
    }

    public String getId() { return id; }
    public String getTemplateId() { return templateId; }
    public String getPhaseTemplateId() { return phaseTemplateId; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public int getSequenceNo() { return sequenceNo; }
    public String getDescription() { return description; }
    public int getPlannedOffsetDays() { return plannedOffsetDays; }
    public ProjectTemplateAppliesToType getAppliesToType() { return appliesToType; }
    public String getStructureLevelTemplateId() { return structureLevelTemplateId; }
    public ProjectOrganizationRoleType getOwnerOrganizationRole() { return ownerOrganizationRole; }
    public ProjectVisibilityScope getVisibilityScope() { return visibilityScope; }
}
