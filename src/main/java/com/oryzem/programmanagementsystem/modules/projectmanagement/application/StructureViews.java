package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectStructureNodeStatus;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectVisibilityScope;
import java.util.List;

public final class StructureViews {

    private StructureViews() {
    }

    public record ProjectStructureLevelView(
            String id,
            int sequence,
            String name,
            String code,
            boolean allowsChildren,
            boolean allowsMilestones,
            boolean allowsDeliverables) {
    }

    public record ProjectStructureNodeView(
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

    public record ProjectStructureTreeView(
            String projectId,
            List<ProjectStructureLevelView> levels,
            List<ProjectStructureNodeView> nodes) {
    }

    public record ProjectStructureTemplateSummaryView(
            String id,
            String name,
            String frameworkType,
            int version,
            boolean active) {
    }

    public record ProjectStructureTemplateDetailView(
            String id,
            String name,
            String frameworkType,
            int version,
            boolean active,
            List<ProjectStructureLevelView> levels,
            List<TemplateViews.ProjectTemplateSummaryView> projectTemplates,
            List<TemplateViews.ProjectTemplateMilestoneTemplateView> milestoneTemplates,
            List<TemplateViews.ProjectTemplateDeliverableTemplateView> deliverableTemplates) {
    }
}
