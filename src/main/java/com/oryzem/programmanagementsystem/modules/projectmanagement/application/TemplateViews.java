package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.DeliverableType;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectFrameworkType;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectOrganizationRoleType;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectPriority;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectTemplateAppliesToType;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectTemplateStatus;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectVisibilityScope;
import java.time.Instant;

public final class TemplateViews {

    private TemplateViews() {
    }

    public record ProjectTemplateSummaryView(
            String id,
            String name,
            ProjectFrameworkType frameworkType,
            int version,
            ProjectTemplateStatus status,
            boolean isDefault,
            String structureTemplateId) {
    }

    public record ProjectTemplateDetailView(
            String id,
            String name,
            ProjectFrameworkType frameworkType,
            int version,
            ProjectTemplateStatus status,
            boolean isDefault,
            String structureTemplateId,
            Instant createdAt) {
    }

    public record ProjectPhaseTemplateView(
            String id,
            String templateId,
            int sequence,
            String name,
            String description,
            Integer plannedStartOffsetDays,
            int plannedEndOffsetDays) {
    }

    public record ProjectTemplateMilestoneTemplateView(
            String id,
            String phaseTemplateId,
            String code,
            String name,
            int sequence,
            String description,
            int plannedOffsetDays,
            ProjectTemplateAppliesToType appliesToType,
            String structureLevelTemplateId,
            ProjectOrganizationRoleType ownerOrganizationRole,
            ProjectVisibilityScope visibilityScope) {
    }

    public record ProjectTemplateDeliverableTemplateView(
            String id,
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
            ProjectPriority priority,
            ProjectVisibilityScope visibilityScope) {
    }
}
