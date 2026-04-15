package com.oryzem.programmanagementsystem.modules.projectmanagement.application.model.read;

import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectFrameworkType;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectStructureNodeStatus;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectVisibilityScope;
import java.util.List;

public final class StructureReadModels {

    private StructureReadModels() {
    }

    public record ProjectStructureLevelReadModel(
            String id,
            int sequence,
            String name,
            String code,
            boolean allowsChildren,
            boolean allowsMilestones,
            boolean allowsDeliverables) {
    }

    public record ProjectStructureNodeReadModel(
            String id,
            String levelTemplateId,
            String parentNodeId,
            String name,
            String code,
            int sequence,
            String ownerOrganizationId,
            String responsibleUserId,
            ProjectStructureNodeStatus status,
            ProjectVisibilityScope visibilityScope,
            long version) {
    }

    public record ProjectStructureTreeReadModel(
            String projectId,
            List<ProjectStructureLevelReadModel> levels,
            List<ProjectStructureNodeReadModel> nodes) {
    }

    public record ProjectStructureTemplateListReadModel(
            String id,
            String name,
            ProjectFrameworkType frameworkType,
            int version,
            boolean active) {
    }

    public record ProjectStructureTemplateDetailReadModel(
            String id,
            String name,
            ProjectFrameworkType frameworkType,
            int version,
            boolean active,
            List<ProjectStructureLevelReadModel> levels,
            List<TemplateReadModels.ProjectTemplateListReadModel> projectTemplates,
            List<TemplateReadModels.ProjectMilestoneTemplateReadModel> milestoneTemplates,
            List<TemplateReadModels.ProjectDeliverableTemplateReadModel> deliverableTemplates) {
    }
}
