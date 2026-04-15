package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.model.read.TemplateReadModels;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.DeliverableTemplateAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectMilestoneTemplateAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectPhaseTemplateAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectTemplateAggregate;
import org.springframework.stereotype.Component;

@Component
public class ProjectTemplateViewMapper {

    public TemplateReadModels.ProjectTemplateListReadModel toProjectTemplateListReadModel(ProjectTemplateAggregate aggregate) {
        return new TemplateReadModels.ProjectTemplateListReadModel(
                aggregate.id(),
                aggregate.name(),
                aggregate.frameworkType(),
                aggregate.version(),
                aggregate.status(),
                aggregate.isDefault(),
                aggregate.structureTemplateId());
    }

    public TemplateViews.ProjectTemplateSummaryView toProjectTemplateSummaryView(ProjectTemplateAggregate aggregate) {
        return new TemplateViews.ProjectTemplateSummaryView(
                aggregate.id(),
                aggregate.name(),
                aggregate.frameworkType(),
                aggregate.version(),
                aggregate.status(),
                aggregate.isDefault(),
                aggregate.structureTemplateId());
    }

    public TemplateReadModels.ProjectTemplateDetailReadModel toProjectTemplateDetailReadModel(ProjectTemplateAggregate aggregate) {
        return new TemplateReadModels.ProjectTemplateDetailReadModel(
                aggregate.id(),
                aggregate.name(),
                aggregate.frameworkType(),
                aggregate.version(),
                aggregate.status(),
                aggregate.isDefault(),
                aggregate.structureTemplateId(),
                aggregate.createdAt());
    }

    public TemplateViews.ProjectTemplateDetailView toProjectTemplateDetailView(ProjectTemplateAggregate aggregate) {
        return new TemplateViews.ProjectTemplateDetailView(
                aggregate.id(),
                aggregate.name(),
                aggregate.frameworkType(),
                aggregate.version(),
                aggregate.status(),
                aggregate.isDefault(),
                aggregate.structureTemplateId(),
                aggregate.createdAt());
    }

    public TemplateReadModels.ProjectPhaseTemplateReadModel toProjectPhaseTemplateReadModel(ProjectPhaseTemplateAggregate aggregate) {
        return new TemplateReadModels.ProjectPhaseTemplateReadModel(
                aggregate.id(),
                aggregate.templateId(),
                aggregate.sequenceNo(),
                aggregate.name(),
                aggregate.description(),
                aggregate.plannedStartOffsetDays(),
                aggregate.plannedEndOffsetDays());
    }

    public TemplateViews.ProjectPhaseTemplateView toPhaseTemplateView(ProjectPhaseTemplateAggregate aggregate) {
        return new TemplateViews.ProjectPhaseTemplateView(
                aggregate.id(),
                aggregate.templateId(),
                aggregate.sequenceNo(),
                aggregate.name(),
                aggregate.description(),
                aggregate.plannedStartOffsetDays(),
                aggregate.plannedEndOffsetDays());
    }

    public TemplateReadModels.ProjectMilestoneTemplateReadModel toProjectMilestoneTemplateReadModel(ProjectMilestoneTemplateAggregate aggregate) {
        return new TemplateReadModels.ProjectMilestoneTemplateReadModel(
                aggregate.id(),
                aggregate.phaseTemplateId(),
                aggregate.code(),
                aggregate.name(),
                aggregate.sequenceNo(),
                aggregate.description(),
                aggregate.plannedOffsetDays(),
                aggregate.appliesToType(),
                aggregate.structureLevelTemplateId(),
                aggregate.ownerOrganizationRole(),
                aggregate.visibilityScope());
    }

    public TemplateViews.ProjectTemplateMilestoneTemplateView toMilestoneTemplateView(ProjectMilestoneTemplateAggregate aggregate) {
        return new TemplateViews.ProjectTemplateMilestoneTemplateView(
                aggregate.id(),
                aggregate.phaseTemplateId(),
                aggregate.code(),
                aggregate.name(),
                aggregate.sequenceNo(),
                aggregate.description(),
                aggregate.plannedOffsetDays(),
                aggregate.appliesToType(),
                aggregate.structureLevelTemplateId(),
                aggregate.ownerOrganizationRole(),
                aggregate.visibilityScope());
    }

    public TemplateReadModels.ProjectDeliverableTemplateReadModel toProjectDeliverableTemplateReadModel(DeliverableTemplateAggregate aggregate) {
        return new TemplateReadModels.ProjectDeliverableTemplateReadModel(
                aggregate.id(),
                aggregate.phaseTemplateId(),
                aggregate.milestoneTemplateId(),
                aggregate.code(),
                aggregate.name(),
                aggregate.description(),
                aggregate.deliverableType(),
                aggregate.requiredDocument(),
                aggregate.plannedDueOffsetDays(),
                aggregate.appliesToType(),
                aggregate.structureLevelTemplateId(),
                aggregate.responsibleOrganizationRole(),
                aggregate.approverOrganizationRole(),
                aggregate.priority(),
                aggregate.visibilityScope());
    }

    public TemplateViews.ProjectTemplateDeliverableTemplateView toDeliverableTemplateView(DeliverableTemplateAggregate aggregate) {
        return new TemplateViews.ProjectTemplateDeliverableTemplateView(
                aggregate.id(),
                aggregate.phaseTemplateId(),
                aggregate.milestoneTemplateId(),
                aggregate.code(),
                aggregate.name(),
                aggregate.description(),
                aggregate.deliverableType(),
                aggregate.requiredDocument(),
                aggregate.plannedDueOffsetDays(),
                aggregate.appliesToType(),
                aggregate.structureLevelTemplateId(),
                aggregate.responsibleOrganizationRole(),
                aggregate.approverOrganizationRole(),
                aggregate.priority(),
                aggregate.visibilityScope());
    }
}
