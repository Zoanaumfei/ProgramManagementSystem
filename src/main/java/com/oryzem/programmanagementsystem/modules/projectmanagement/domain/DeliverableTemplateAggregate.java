package com.oryzem.programmanagementsystem.modules.projectmanagement.domain;

public record DeliverableTemplateAggregate(
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

    public DeliverableTemplateAggregate update(
            String nextPhaseTemplateId,
            String nextMilestoneTemplateId,
            String nextCode,
            String nextName,
            String nextDescription,
            DeliverableType nextDeliverableType,
            boolean nextRequiredDocument,
            int nextPlannedDueOffsetDays,
            ProjectTemplateAppliesToType nextAppliesToType,
            String nextStructureLevelTemplateId,
            ProjectOrganizationRoleType nextResponsibleOrganizationRole,
            ProjectOrganizationRoleType nextApproverOrganizationRole,
            ProjectVisibilityScope nextVisibilityScope,
            ProjectPriority nextPriority) {
        return new DeliverableTemplateAggregate(
                id,
                templateId,
                nextPhaseTemplateId,
                nextMilestoneTemplateId,
                nextCode,
                nextName,
                nextDescription,
                nextDeliverableType,
                nextRequiredDocument,
                nextPlannedDueOffsetDays,
                nextAppliesToType,
                nextStructureLevelTemplateId,
                nextResponsibleOrganizationRole,
                nextApproverOrganizationRole,
                nextVisibilityScope,
                nextPriority);
    }
}
