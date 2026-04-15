package com.oryzem.programmanagementsystem.modules.projectmanagement.api;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.TemplateViews;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.model.read.TemplateReadModels;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.DeliverableType;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectFrameworkType;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectOrganizationRoleType;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectPriority;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectTemplateAppliesToType;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectTemplateStatus;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectVisibilityScope;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public final class ProjectTemplateDtos {

    private ProjectTemplateDtos() {
    }

    public record CreateProjectTemplateRequest(
            @NotBlank String name,
            @NotNull ProjectFrameworkType frameworkType,
            int version,
            @NotNull ProjectTemplateStatus status,
            boolean isDefault,
            @NotBlank String structureTemplateId) {
    }

    public record UpdateProjectTemplateRequest(
            @NotBlank String name,
            @NotNull ProjectTemplateStatus status,
            boolean isDefault,
            @NotBlank String structureTemplateId) {
    }

    public record CreateProjectPhaseTemplateRequest(
            @NotBlank String name,
            String description,
            Integer plannedStartOffsetDays,
            int plannedEndOffsetDays) {
    }

    public record UpdateProjectPhaseTemplateRequest(
            @NotBlank String name,
            String description,
            Integer plannedStartOffsetDays,
            int plannedEndOffsetDays) {
    }

    public record CreateProjectMilestoneTemplateRequest(
            String phaseTemplateId,
            @NotBlank String code,
            @NotBlank String name,
            String description,
            int plannedOffsetDays,
            @NotNull ProjectTemplateAppliesToType appliesToType,
            @NotBlank String structureLevelTemplateId,
            ProjectOrganizationRoleType ownerOrganizationRole,
            @NotNull ProjectVisibilityScope visibilityScope) {
    }

    public record UpdateProjectMilestoneTemplateRequest(
            String phaseTemplateId,
            @NotBlank String code,
            @NotBlank String name,
            String description,
            int plannedOffsetDays,
            @NotNull ProjectTemplateAppliesToType appliesToType,
            @NotBlank String structureLevelTemplateId,
            ProjectOrganizationRoleType ownerOrganizationRole,
            @NotNull ProjectVisibilityScope visibilityScope) {
    }

    public record CreateDeliverableTemplateRequest(
            String phaseTemplateId,
            String milestoneTemplateId,
            @NotBlank String code,
            @NotBlank String name,
            String description,
            @NotNull DeliverableType deliverableType,
            boolean requiredDocument,
            int plannedDueOffsetDays,
            @NotNull ProjectTemplateAppliesToType appliesToType,
            @NotBlank String structureLevelTemplateId,
            ProjectOrganizationRoleType responsibleOrganizationRole,
            ProjectOrganizationRoleType approverOrganizationRole,
            @NotNull ProjectVisibilityScope visibilityScope,
            @NotNull ProjectPriority priority) {
    }

    public record UpdateDeliverableTemplateRequest(
            String phaseTemplateId,
            String milestoneTemplateId,
            @NotBlank String code,
            @NotBlank String name,
            String description,
            @NotNull DeliverableType deliverableType,
            boolean requiredDocument,
            int plannedDueOffsetDays,
            @NotNull ProjectTemplateAppliesToType appliesToType,
            @NotBlank String structureLevelTemplateId,
            ProjectOrganizationRoleType responsibleOrganizationRole,
            ProjectOrganizationRoleType approverOrganizationRole,
            @NotNull ProjectVisibilityScope visibilityScope,
            @NotNull ProjectPriority priority) {
    }

    public record ProjectTemplateSummaryResponse(String id, String name, ProjectFrameworkType frameworkType, int version, ProjectTemplateStatus status, boolean isDefault, String structureTemplateId) {
        static ProjectTemplateSummaryResponse from(TemplateViews.ProjectTemplateSummaryView view) {
            return new ProjectTemplateSummaryResponse(view.id(), view.name(), view.frameworkType(), view.version(), view.status(), view.isDefault(), view.structureTemplateId());
        }

        static ProjectTemplateSummaryResponse from(TemplateReadModels.ProjectTemplateListReadModel readModel) {
            return new ProjectTemplateSummaryResponse(readModel.id(), readModel.name(), readModel.frameworkType(), readModel.version(), readModel.status(), readModel.isDefault(), readModel.structureTemplateId());
        }
    }

    public record ProjectTemplateDetailResponse(String id, String name, ProjectFrameworkType frameworkType, int version, ProjectTemplateStatus status, boolean isDefault, String structureTemplateId, Instant createdAt) {
        static ProjectTemplateDetailResponse from(TemplateViews.ProjectTemplateDetailView view) {
            return new ProjectTemplateDetailResponse(view.id(), view.name(), view.frameworkType(), view.version(), view.status(), view.isDefault(), view.structureTemplateId(), view.createdAt());
        }

        static ProjectTemplateDetailResponse from(TemplateReadModels.ProjectTemplateDetailReadModel readModel) {
            return new ProjectTemplateDetailResponse(readModel.id(), readModel.name(), readModel.frameworkType(), readModel.version(), readModel.status(), readModel.isDefault(), readModel.structureTemplateId(), readModel.createdAt());
        }
    }

    public record ProjectPhaseTemplateResponse(String id, String templateId, int sequence, String name, String description, Integer plannedStartOffsetDays, int plannedEndOffsetDays) {
        static ProjectPhaseTemplateResponse from(TemplateViews.ProjectPhaseTemplateView view) {
            return new ProjectPhaseTemplateResponse(view.id(), view.templateId(), view.sequence(), view.name(), view.description(), view.plannedStartOffsetDays(), view.plannedEndOffsetDays());
        }

        static ProjectPhaseTemplateResponse from(TemplateReadModels.ProjectPhaseTemplateReadModel readModel) {
            return new ProjectPhaseTemplateResponse(readModel.id(), readModel.templateId(), readModel.sequence(), readModel.name(), readModel.description(), readModel.plannedStartOffsetDays(), readModel.plannedEndOffsetDays());
        }
    }

    public record ProjectTemplateMilestoneTemplateResponse(String id, String phaseTemplateId, String code, String name, int sequence, String description, int plannedOffsetDays, ProjectTemplateAppliesToType appliesToType, String structureLevelTemplateId, ProjectOrganizationRoleType ownerOrganizationRole, ProjectVisibilityScope visibilityScope) {
        static ProjectTemplateMilestoneTemplateResponse from(TemplateViews.ProjectTemplateMilestoneTemplateView view) {
            return new ProjectTemplateMilestoneTemplateResponse(view.id(), view.phaseTemplateId(), view.code(), view.name(), view.sequence(), view.description(), view.plannedOffsetDays(), view.appliesToType(), view.structureLevelTemplateId(), view.ownerOrganizationRole(), view.visibilityScope());
        }

        static ProjectTemplateMilestoneTemplateResponse from(TemplateReadModels.ProjectMilestoneTemplateReadModel readModel) {
            return new ProjectTemplateMilestoneTemplateResponse(readModel.id(), readModel.phaseTemplateId(), readModel.code(), readModel.name(), readModel.sequence(), readModel.description(), readModel.plannedOffsetDays(), readModel.appliesToType(), readModel.structureLevelTemplateId(), readModel.ownerOrganizationRole(), readModel.visibilityScope());
        }
    }

    public record ProjectTemplateDeliverableTemplateResponse(String id, String phaseTemplateId, String milestoneTemplateId, String code, String name, String description, DeliverableType deliverableType, boolean requiredDocument, int plannedDueOffsetDays, ProjectTemplateAppliesToType appliesToType, String structureLevelTemplateId, ProjectOrganizationRoleType responsibleOrganizationRole, ProjectOrganizationRoleType approverOrganizationRole, ProjectPriority priority, ProjectVisibilityScope visibilityScope) {
        static ProjectTemplateDeliverableTemplateResponse from(TemplateViews.ProjectTemplateDeliverableTemplateView view) {
            return new ProjectTemplateDeliverableTemplateResponse(view.id(), view.phaseTemplateId(), view.milestoneTemplateId(), view.code(), view.name(), view.description(), view.deliverableType(), view.requiredDocument(), view.plannedDueOffsetDays(), view.appliesToType(), view.structureLevelTemplateId(), view.responsibleOrganizationRole(), view.approverOrganizationRole(), view.priority(), view.visibilityScope());
        }

        static ProjectTemplateDeliverableTemplateResponse from(TemplateReadModels.ProjectDeliverableTemplateReadModel readModel) {
            return new ProjectTemplateDeliverableTemplateResponse(readModel.id(), readModel.phaseTemplateId(), readModel.milestoneTemplateId(), readModel.code(), readModel.name(), readModel.description(), readModel.deliverableType(), readModel.requiredDocument(), readModel.plannedDueOffsetDays(), readModel.appliesToType(), readModel.structureLevelTemplateId(), readModel.responsibleOrganizationRole(), readModel.approverOrganizationRole(), readModel.priority(), readModel.visibilityScope());
        }
    }
}
