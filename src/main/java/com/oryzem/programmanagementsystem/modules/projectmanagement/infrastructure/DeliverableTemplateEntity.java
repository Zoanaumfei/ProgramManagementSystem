package com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure;

import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.DeliverableType;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectOrganizationRoleType;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectPriority;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectTemplateAppliesToType;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectVisibilityScope;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "deliverable_template")
public class DeliverableTemplateEntity {
    @Id
    @Column(length = 64, nullable = false)
    private String id;
    @Column(name = "template_id", length = 64, nullable = false)
    private String templateId;
    @Column(name = "phase_template_id", length = 64)
    private String phaseTemplateId;
    @Column(name = "milestone_template_id", length = 64)
    private String milestoneTemplateId;
    @Column(length = 64, nullable = false)
    private String code;
    @Column(length = 160, nullable = false)
    private String name;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Enumerated(EnumType.STRING)
    @Column(name = "deliverable_type", length = 32, nullable = false)
    private DeliverableType deliverableType;
    @Column(name = "required_document", nullable = false)
    private boolean requiredDocument;
    @Column(name = "planned_due_offset_days", nullable = false)
    private int plannedDueOffsetDays;
    @Enumerated(EnumType.STRING)
    @Column(name = "applies_to_type", length = 32, nullable = false)
    private ProjectTemplateAppliesToType appliesToType;
    @Column(name = "structure_level_template_id", length = 64)
    private String structureLevelTemplateId;
    @Enumerated(EnumType.STRING)
    @Column(name = "responsible_organization_role", length = 32)
    private ProjectOrganizationRoleType responsibleOrganizationRole;
    @Enumerated(EnumType.STRING)
    @Column(name = "approver_organization_role", length = 32)
    private ProjectOrganizationRoleType approverOrganizationRole;
    @Enumerated(EnumType.STRING)
    @Column(name = "visibility_scope", length = 32, nullable = false)
    private ProjectVisibilityScope visibilityScope;
    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private ProjectPriority priority;

    protected DeliverableTemplateEntity() {}

    public static DeliverableTemplateEntity create(
            String id,
            String templateId,
            String phaseTemplateId,
            String milestoneTemplateId,
            String code,
            String name,
            String description,
            DeliverableType deliverableType,
            boolean requiredDocument,
            int plannedDueOffsetDays,
            ProjectTemplateAppliesToType appliesToType,
            String structureLevelTemplateId,
            ProjectOrganizationRoleType responsibleOrganizationRole,
            ProjectOrganizationRoleType approverOrganizationRole,
            ProjectVisibilityScope visibilityScope,
            ProjectPriority priority) {
        DeliverableTemplateEntity entity = new DeliverableTemplateEntity();
        entity.id = id;
        entity.templateId = templateId;
        entity.phaseTemplateId = phaseTemplateId;
        entity.milestoneTemplateId = milestoneTemplateId;
        entity.code = code;
        entity.name = name;
        entity.description = description;
        entity.deliverableType = deliverableType;
        entity.requiredDocument = requiredDocument;
        entity.plannedDueOffsetDays = plannedDueOffsetDays;
        entity.appliesToType = appliesToType;
        entity.structureLevelTemplateId = structureLevelTemplateId;
        entity.responsibleOrganizationRole = responsibleOrganizationRole;
        entity.approverOrganizationRole = approverOrganizationRole;
        entity.visibilityScope = visibilityScope;
        entity.priority = priority;
        return entity;
    }

    public void applyUpdate(
            String phaseTemplateId,
            String milestoneTemplateId,
            String code,
            String name,
            String description,
            DeliverableType deliverableType,
            boolean requiredDocument,
            int plannedDueOffsetDays,
            ProjectTemplateAppliesToType appliesToType,
            String structureLevelTemplateId,
            ProjectOrganizationRoleType responsibleOrganizationRole,
            ProjectOrganizationRoleType approverOrganizationRole,
            ProjectVisibilityScope visibilityScope,
            ProjectPriority priority) {
        this.phaseTemplateId = phaseTemplateId;
        this.milestoneTemplateId = milestoneTemplateId;
        this.code = code;
        this.name = name;
        this.description = description;
        this.deliverableType = deliverableType;
        this.requiredDocument = requiredDocument;
        this.plannedDueOffsetDays = plannedDueOffsetDays;
        this.appliesToType = appliesToType;
        this.structureLevelTemplateId = structureLevelTemplateId;
        this.responsibleOrganizationRole = responsibleOrganizationRole;
        this.approverOrganizationRole = approverOrganizationRole;
        this.visibilityScope = visibilityScope;
        this.priority = priority;
    }

    public String getId() { return id; }
    public String getTemplateId() { return templateId; }
    public String getPhaseTemplateId() { return phaseTemplateId; }
    public String getMilestoneTemplateId() { return milestoneTemplateId; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public DeliverableType getDeliverableType() { return deliverableType; }
    public boolean isRequiredDocument() { return requiredDocument; }
    public int getPlannedDueOffsetDays() { return plannedDueOffsetDays; }
    public ProjectTemplateAppliesToType getAppliesToType() { return appliesToType; }
    public String getStructureLevelTemplateId() { return structureLevelTemplateId; }
    public ProjectOrganizationRoleType getResponsibleOrganizationRole() { return responsibleOrganizationRole; }
    public ProjectOrganizationRoleType getApproverOrganizationRole() { return approverOrganizationRole; }
    public ProjectVisibilityScope getVisibilityScope() { return visibilityScope; }
    public ProjectPriority getPriority() { return priority; }
}
