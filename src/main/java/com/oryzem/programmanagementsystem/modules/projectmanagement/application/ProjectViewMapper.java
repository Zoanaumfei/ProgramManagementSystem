package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.model.read.ProjectReadModels;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.model.read.StructureReadModels;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.model.read.SubmissionReadModels;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.model.read.TemplateReadModels;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.*;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ProjectViewMapper {

    private final ProjectCoreViewMapper coreMapper;
    private final ProjectStructureViewMapper structureMapper;
    private final ProjectTemplateViewMapper templateMapper;
    private final ProjectSubmissionViewMapper submissionMapper;

    public ProjectViewMapper() {
        this(
                new ProjectCoreViewMapper(),
                new ProjectStructureViewMapper(),
                new ProjectTemplateViewMapper(),
                new ProjectSubmissionViewMapper());
    }

    public ProjectViewMapper(
            ProjectCoreViewMapper coreMapper,
            ProjectStructureViewMapper structureMapper,
            ProjectTemplateViewMapper templateMapper,
            ProjectSubmissionViewMapper submissionMapper) {
        this.coreMapper = coreMapper;
        this.structureMapper = structureMapper;
        this.templateMapper = templateMapper;
        this.submissionMapper = submissionMapper;
    }

    public ProjectViews.ProjectSummaryView toSummary(ProjectAggregate aggregate) {
        return coreMapper.toSummary(aggregate);
    }

    public ProjectReadModels.ProjectListReadModel toProjectListReadModel(ProjectAggregate aggregate) {
        return coreMapper.toProjectListReadModel(aggregate);
    }

    public ProjectViews.ProjectDetailView toDetail(
            ProjectAggregate aggregate,
            List<ProjectOrganizationAggregate> organizations,
            List<ProjectMemberAggregate> members) {
        return coreMapper.toDetail(aggregate, organizations, members);
    }

    public ProjectReadModels.ProjectDetailReadModel toProjectDetailReadModel(
            ProjectAggregate aggregate,
            List<ProjectOrganizationAggregate> organizations,
            List<ProjectMemberAggregate> members) {
        return coreMapper.toProjectDetailReadModel(aggregate, organizations, members);
    }

    public ProjectViews.ProjectOrganizationView toOrganizationView(ProjectOrganizationAggregate aggregate) {
        return coreMapper.toOrganizationView(aggregate);
    }

    public ProjectReadModels.ProjectOrganizationReadModel toProjectOrganizationReadModel(ProjectOrganizationAggregate aggregate) {
        return coreMapper.toProjectOrganizationReadModel(aggregate);
    }

    public ProjectViews.ProjectMemberView toMemberView(ProjectMemberAggregate aggregate) {
        return coreMapper.toMemberView(aggregate);
    }

    public ProjectReadModels.ProjectMemberReadModel toProjectMemberReadModel(ProjectMemberAggregate aggregate) {
        return coreMapper.toProjectMemberReadModel(aggregate);
    }

    public ProjectViews.ProjectMilestoneView toMilestoneView(ProjectMilestoneAggregate aggregate) {
        return coreMapper.toMilestoneView(aggregate);
    }

    public ProjectReadModels.ProjectMilestoneListReadModel toProjectMilestoneListReadModel(ProjectMilestoneAggregate aggregate) {
        return coreMapper.toProjectMilestoneListReadModel(aggregate);
    }

    public ProjectViews.ProjectDeliverableView toDeliverableView(ProjectDeliverableAggregate aggregate) {
        return coreMapper.toDeliverableView(aggregate);
    }

    public ProjectReadModels.ProjectDeliverableListReadModel toProjectDeliverableListReadModel(ProjectDeliverableAggregate aggregate) {
        return coreMapper.toProjectDeliverableListReadModel(aggregate);
    }

    public ProjectReadModels.ProjectDeliverableDetailReadModel toProjectDeliverableDetailReadModel(ProjectDeliverableAggregate aggregate) {
        return coreMapper.toProjectDeliverableDetailReadModel(aggregate);
    }

    public ProjectReadModels.PendingSubmissionReviewReadModel toPendingSubmissionReviewReadModel(ProjectDeliverableAggregate aggregate) {
        return coreMapper.toPendingSubmissionReviewReadModel(aggregate);
    }

    public StructureViews.ProjectStructureLevelView toStructureLevelView(ProjectStructureLevelTemplateAggregate aggregate) {
        return structureMapper.toStructureLevelView(aggregate);
    }

    public StructureReadModels.ProjectStructureLevelReadModel toProjectStructureLevelReadModel(ProjectStructureLevelTemplateAggregate aggregate) {
        return structureMapper.toProjectStructureLevelReadModel(aggregate);
    }

    public StructureViews.ProjectStructureNodeView toStructureNodeView(ProjectStructureNodeAggregate aggregate) {
        return structureMapper.toStructureNodeView(aggregate);
    }

    public StructureReadModels.ProjectStructureNodeReadModel toProjectStructureNodeReadModel(ProjectStructureNodeAggregate aggregate) {
        return structureMapper.toProjectStructureNodeReadModel(aggregate);
    }

    public TemplateViews.ProjectTemplateSummaryView toProjectTemplateSummaryView(ProjectTemplateAggregate aggregate) {
        return templateMapper.toProjectTemplateSummaryView(aggregate);
    }

    public TemplateReadModels.ProjectTemplateListReadModel toProjectTemplateListReadModel(ProjectTemplateAggregate aggregate) {
        return templateMapper.toProjectTemplateListReadModel(aggregate);
    }

    public TemplateViews.ProjectTemplateDetailView toProjectTemplateDetailView(ProjectTemplateAggregate aggregate) {
        return templateMapper.toProjectTemplateDetailView(aggregate);
    }

    public TemplateReadModels.ProjectTemplateDetailReadModel toProjectTemplateDetailReadModel(ProjectTemplateAggregate aggregate) {
        return templateMapper.toProjectTemplateDetailReadModel(aggregate);
    }

    public TemplateViews.ProjectPhaseTemplateView toPhaseTemplateView(ProjectPhaseTemplateAggregate aggregate) {
        return templateMapper.toPhaseTemplateView(aggregate);
    }

    public TemplateReadModels.ProjectPhaseTemplateReadModel toProjectPhaseTemplateReadModel(ProjectPhaseTemplateAggregate aggregate) {
        return templateMapper.toProjectPhaseTemplateReadModel(aggregate);
    }

    public StructureViews.ProjectStructureTemplateSummaryView toStructureTemplateSummaryView(ProjectStructureTemplateAggregate aggregate) {
        return structureMapper.toStructureTemplateSummaryView(aggregate);
    }

    public StructureReadModels.ProjectStructureTemplateListReadModel toProjectStructureTemplateListReadModel(ProjectStructureTemplateAggregate aggregate) {
        return structureMapper.toProjectStructureTemplateListReadModel(aggregate);
    }

    public TemplateViews.ProjectTemplateMilestoneTemplateView toMilestoneTemplateView(ProjectMilestoneTemplateAggregate aggregate) {
        return templateMapper.toMilestoneTemplateView(aggregate);
    }

    public TemplateReadModels.ProjectMilestoneTemplateReadModel toProjectMilestoneTemplateReadModel(ProjectMilestoneTemplateAggregate aggregate) {
        return templateMapper.toProjectMilestoneTemplateReadModel(aggregate);
    }

    public TemplateViews.ProjectTemplateDeliverableTemplateView toDeliverableTemplateView(DeliverableTemplateAggregate aggregate) {
        return templateMapper.toDeliverableTemplateView(aggregate);
    }

    public TemplateReadModels.ProjectDeliverableTemplateReadModel toProjectDeliverableTemplateReadModel(DeliverableTemplateAggregate aggregate) {
        return templateMapper.toProjectDeliverableTemplateReadModel(aggregate);
    }

    public SubmissionViews.DeliverableSubmissionView toSubmissionView(
            DeliverableSubmissionAggregate aggregate,
            List<DeliverableSubmissionDocumentAggregate> documents) {
        return submissionMapper.toSubmissionView(aggregate, documents);
    }

    public SubmissionReadModels.DeliverableSubmissionReadModel toSubmissionReadModel(
            DeliverableSubmissionAggregate aggregate,
            List<DeliverableSubmissionDocumentAggregate> documents) {
        return submissionMapper.toSubmissionReadModel(aggregate, documents);
    }
}

