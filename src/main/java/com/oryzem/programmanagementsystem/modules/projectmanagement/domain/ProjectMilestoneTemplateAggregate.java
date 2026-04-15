package com.oryzem.programmanagementsystem.modules.projectmanagement.domain;

public record ProjectMilestoneTemplateAggregate(
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

    public ProjectMilestoneTemplateAggregate update(
            String nextPhaseTemplateId,
            String nextCode,
            String nextName,
            String nextDescription,
            int nextPlannedOffsetDays,
            ProjectTemplateAppliesToType nextAppliesToType,
            String nextStructureLevelTemplateId,
            ProjectOrganizationRoleType nextOwnerOrganizationRole,
            ProjectVisibilityScope nextVisibilityScope) {
        return new ProjectMilestoneTemplateAggregate(
                id,
                templateId,
                nextPhaseTemplateId,
                nextCode,
                nextName,
                sequenceNo,
                nextDescription,
                nextPlannedOffsetDays,
                nextAppliesToType,
                nextStructureLevelTemplateId,
                nextOwnerOrganizationRole,
                nextVisibilityScope);
    }
}
