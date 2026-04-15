package com.oryzem.programmanagementsystem.modules.projectmanagement.api;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.StructureViews;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.model.read.StructureReadModels;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectFrameworkType;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectStructureNodeStatus;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectVisibilityScope;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public final class ProjectStructureDtos {

    private ProjectStructureDtos() {
    }

    public record CreateProjectStructureNodeRequest(
            @NotBlank String parentNodeId,
            @NotBlank String name,
            @NotBlank String code,
            String ownerOrganizationId,
            String responsibleUserId,
            ProjectVisibilityScope visibilityScope) {
    }

    public record UpdateProjectStructureNodeRequest(
            @NotBlank String name,
            @NotBlank String code,
            String ownerOrganizationId,
            String responsibleUserId,
            ProjectVisibilityScope visibilityScope,
            long version) {
    }

    public record MoveProjectStructureNodeRequest(
            @NotBlank String newParentNodeId,
            long version) {
    }

    public record CreateProjectStructureTemplateRequest(
            @NotBlank String name,
            @NotNull ProjectFrameworkType frameworkType,
            int version,
            boolean active) {
    }

    public record UpdateProjectStructureTemplateRequest(@NotBlank String name) {
    }

    public record CreateProjectStructureLevelTemplateRequest(
            @NotBlank String name,
            @NotBlank String code,
            boolean allowsChildren,
            boolean allowsMilestones,
            boolean allowsDeliverables) {
    }

    public record UpdateProjectStructureLevelTemplateRequest(
            @NotBlank String name,
            @NotBlank String code,
            boolean allowsChildren,
            boolean allowsMilestones,
            boolean allowsDeliverables) {
    }

    public record ReorderProjectStructureLevelsRequest(@NotNull List<String> orderedLevelIds) {
    }

    public record ProjectStructureLevelResponse(String id, int sequence, String name, String code, boolean allowsChildren, boolean allowsMilestones, boolean allowsDeliverables) {
        static ProjectStructureLevelResponse from(StructureViews.ProjectStructureLevelView view) {
            return new ProjectStructureLevelResponse(view.id(), view.sequence(), view.name(), view.code(), view.allowsChildren(), view.allowsMilestones(), view.allowsDeliverables());
        }

        static ProjectStructureLevelResponse from(StructureReadModels.ProjectStructureLevelReadModel readModel) {
            return new ProjectStructureLevelResponse(readModel.id(), readModel.sequence(), readModel.name(), readModel.code(), readModel.allowsChildren(), readModel.allowsMilestones(), readModel.allowsDeliverables());
        }
    }

    public record ProjectStructureNodeResponse(String id, String levelTemplateId, String parentNodeId, String name, String code, int sequence, String ownerOrganizationId, String responsibleUserId, ProjectStructureNodeStatus status, ProjectVisibilityScope visibilityScope, long version) {
        static ProjectStructureNodeResponse from(StructureViews.ProjectStructureNodeView view) {
            return new ProjectStructureNodeResponse(view.id(), view.levelTemplateId(), view.parentNodeId(), view.name(), view.code(), view.sequence(), view.ownerOrganizationId(), view.responsibleUserId(), view.status(), view.visibilityScope(), view.version());
        }

        static ProjectStructureNodeResponse from(StructureReadModels.ProjectStructureNodeReadModel readModel) {
            return new ProjectStructureNodeResponse(readModel.id(), readModel.levelTemplateId(), readModel.parentNodeId(), readModel.name(), readModel.code(), readModel.sequence(), readModel.ownerOrganizationId(), readModel.responsibleUserId(), readModel.status(), readModel.visibilityScope(), readModel.version());
        }
    }

    public record ProjectStructureTreeResponse(String projectId, List<ProjectStructureLevelResponse> levels, List<ProjectStructureNodeResponse> nodes) {
        static ProjectStructureTreeResponse from(StructureViews.ProjectStructureTreeView view) {
            return new ProjectStructureTreeResponse(view.projectId(), view.levels().stream().map(ProjectStructureLevelResponse::from).toList(), view.nodes().stream().map(ProjectStructureNodeResponse::from).toList());
        }

        static ProjectStructureTreeResponse from(StructureReadModels.ProjectStructureTreeReadModel readModel) {
            return new ProjectStructureTreeResponse(readModel.projectId(), readModel.levels().stream().map(ProjectStructureLevelResponse::from).toList(), readModel.nodes().stream().map(ProjectStructureNodeResponse::from).toList());
        }
    }

    public record ProjectStructureTemplateSummaryResponse(String id, String name, ProjectFrameworkType frameworkType, int version, boolean active) {
        static ProjectStructureTemplateSummaryResponse from(StructureViews.ProjectStructureTemplateSummaryView view) {
            return new ProjectStructureTemplateSummaryResponse(view.id(), view.name(), view.frameworkType(), view.version(), view.active());
        }

        static ProjectStructureTemplateSummaryResponse from(StructureReadModels.ProjectStructureTemplateListReadModel readModel) {
            return new ProjectStructureTemplateSummaryResponse(readModel.id(), readModel.name(), readModel.frameworkType(), readModel.version(), readModel.active());
        }
    }

    public record ProjectStructureTemplateDetailResponse(String id, String name, ProjectFrameworkType frameworkType, int version, boolean active, List<ProjectStructureLevelResponse> levels, List<ProjectTemplateDtos.ProjectTemplateSummaryResponse> projectTemplates, List<ProjectTemplateDtos.ProjectTemplateMilestoneTemplateResponse> milestoneTemplates, List<ProjectTemplateDtos.ProjectTemplateDeliverableTemplateResponse> deliverableTemplates) {
        static ProjectStructureTemplateDetailResponse from(StructureViews.ProjectStructureTemplateDetailView view) {
            return new ProjectStructureTemplateDetailResponse(
                    view.id(),
                    view.name(),
                    view.frameworkType(),
                    view.version(),
                    view.active(),
                    view.levels().stream().map(ProjectStructureLevelResponse::from).toList(),
                    view.projectTemplates().stream().map(ProjectTemplateDtos.ProjectTemplateSummaryResponse::from).toList(),
                    view.milestoneTemplates().stream().map(ProjectTemplateDtos.ProjectTemplateMilestoneTemplateResponse::from).toList(),
                    view.deliverableTemplates().stream().map(ProjectTemplateDtos.ProjectTemplateDeliverableTemplateResponse::from).toList());
        }

        static ProjectStructureTemplateDetailResponse from(StructureReadModels.ProjectStructureTemplateDetailReadModel readModel) {
            return new ProjectStructureTemplateDetailResponse(
                    readModel.id(),
                    readModel.name(),
                    readModel.frameworkType(),
                    readModel.version(),
                    readModel.active(),
                    readModel.levels().stream().map(ProjectStructureLevelResponse::from).toList(),
                    readModel.projectTemplates().stream().map(ProjectTemplateDtos.ProjectTemplateSummaryResponse::from).toList(),
                    readModel.milestoneTemplates().stream().map(ProjectTemplateDtos.ProjectTemplateMilestoneTemplateResponse::from).toList(),
                    readModel.deliverableTemplates().stream().map(ProjectTemplateDtos.ProjectTemplateDeliverableTemplateResponse::from).toList());
        }
    }
}
