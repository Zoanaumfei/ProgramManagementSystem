package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.model.read.StructureReadModels;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectStructureLevelTemplateAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectStructureNodeAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectStructureTemplateAggregate;
import org.springframework.stereotype.Component;

@Component
public class ProjectStructureViewMapper {

    public StructureReadModels.ProjectStructureLevelReadModel toProjectStructureLevelReadModel(ProjectStructureLevelTemplateAggregate aggregate) {
        return new StructureReadModels.ProjectStructureLevelReadModel(
                aggregate.id(),
                aggregate.sequenceNo(),
                aggregate.name(),
                aggregate.code(),
                aggregate.allowsChildren(),
                aggregate.allowsMilestones(),
                aggregate.allowsDeliverables());
    }

    public StructureViews.ProjectStructureLevelView toStructureLevelView(ProjectStructureLevelTemplateAggregate aggregate) {
        return new StructureViews.ProjectStructureLevelView(
                aggregate.id(),
                aggregate.sequenceNo(),
                aggregate.name(),
                aggregate.code(),
                aggregate.allowsChildren(),
                aggregate.allowsMilestones(),
                aggregate.allowsDeliverables());
    }

    public StructureReadModels.ProjectStructureNodeReadModel toProjectStructureNodeReadModel(ProjectStructureNodeAggregate aggregate) {
        return new StructureReadModels.ProjectStructureNodeReadModel(
                aggregate.id(),
                aggregate.levelTemplateId(),
                aggregate.parentNodeId(),
                aggregate.name(),
                aggregate.code(),
                aggregate.sequenceNo(),
                aggregate.ownerOrganizationId(),
                aggregate.responsibleUserId(),
                aggregate.status(),
                aggregate.visibilityScope(),
                aggregate.version());
    }

    public StructureViews.ProjectStructureNodeView toStructureNodeView(ProjectStructureNodeAggregate aggregate) {
        return new StructureViews.ProjectStructureNodeView(
                aggregate.id(),
                aggregate.levelTemplateId(),
                aggregate.parentNodeId(),
                aggregate.name(),
                aggregate.code(),
                aggregate.sequenceNo(),
                aggregate.ownerOrganizationId(),
                aggregate.responsibleUserId(),
                aggregate.status(),
                aggregate.visibilityScope(),
                aggregate.version());
    }

    public StructureReadModels.ProjectStructureTemplateListReadModel toProjectStructureTemplateListReadModel(ProjectStructureTemplateAggregate aggregate) {
        return new StructureReadModels.ProjectStructureTemplateListReadModel(
                aggregate.id(),
                aggregate.name(),
                aggregate.frameworkType(),
                aggregate.version(),
                aggregate.active());
    }

    public StructureViews.ProjectStructureTemplateSummaryView toStructureTemplateSummaryView(ProjectStructureTemplateAggregate aggregate) {
        return new StructureViews.ProjectStructureTemplateSummaryView(
                aggregate.id(),
                aggregate.name(),
                aggregate.frameworkType(),
                aggregate.version(),
                aggregate.active());
    }
}
