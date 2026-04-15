package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.model.read.ProjectReadModels;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectDeliverableAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectMemberAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectMilestoneAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectOrganizationAggregate;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ProjectCoreViewMapper {

    public ProjectReadModels.ProjectListReadModel toProjectListReadModel(ProjectAggregate aggregate) {
        return new ProjectReadModels.ProjectListReadModel(
                aggregate.id(),
                aggregate.code(),
                aggregate.name(),
                aggregate.frameworkType(),
                aggregate.status(),
                aggregate.visibilityScope(),
                aggregate.leadOrganizationId(),
                aggregate.customerOrganizationId(),
                aggregate.plannedStartDate(),
                aggregate.plannedEndDate(),
                aggregate.createdAt());
    }

    public ProjectViews.ProjectSummaryView toSummary(ProjectAggregate aggregate) {
        return new ProjectViews.ProjectSummaryView(
                aggregate.id(),
                aggregate.code(),
                aggregate.name(),
                aggregate.frameworkType(),
                aggregate.status(),
                aggregate.visibilityScope(),
                aggregate.leadOrganizationId(),
                aggregate.customerOrganizationId(),
                aggregate.plannedStartDate(),
                aggregate.plannedEndDate(),
                aggregate.createdAt());
    }

    public ProjectReadModels.ProjectDetailReadModel toProjectDetailReadModel(
            ProjectAggregate aggregate,
            List<ProjectOrganizationAggregate> organizations,
            List<ProjectMemberAggregate> members) {
        return new ProjectReadModels.ProjectDetailReadModel(
                aggregate.id(),
                aggregate.code(),
                aggregate.name(),
                aggregate.description(),
                aggregate.frameworkType(),
                aggregate.templateId(),
                aggregate.templateVersion(),
                aggregate.leadOrganizationId(),
                aggregate.customerOrganizationId(),
                aggregate.status(),
                aggregate.visibilityScope(),
                aggregate.plannedStartDate(),
                aggregate.plannedEndDate(),
                aggregate.actualStartDate(),
                aggregate.actualEndDate(),
                aggregate.createdByUserId(),
                aggregate.createdAt(),
                aggregate.updatedAt(),
                aggregate.version(),
                organizations.stream().map(this::toProjectOrganizationReadModel).toList(),
                members.stream().map(this::toProjectMemberReadModel).toList());
    }

    public ProjectViews.ProjectDetailView toDetail(
            ProjectAggregate aggregate,
            List<ProjectOrganizationAggregate> organizations,
            List<ProjectMemberAggregate> members) {
        return new ProjectViews.ProjectDetailView(
                aggregate.id(),
                aggregate.code(),
                aggregate.name(),
                aggregate.description(),
                aggregate.frameworkType(),
                aggregate.templateId(),
                aggregate.templateVersion(),
                aggregate.leadOrganizationId(),
                aggregate.customerOrganizationId(),
                aggregate.status(),
                aggregate.visibilityScope(),
                aggregate.plannedStartDate(),
                aggregate.plannedEndDate(),
                aggregate.actualStartDate(),
                aggregate.actualEndDate(),
                aggregate.createdByUserId(),
                aggregate.createdAt(),
                aggregate.updatedAt(),
                aggregate.version(),
                organizations.stream().map(this::toOrganizationView).toList(),
                members.stream().map(this::toMemberView).toList());
    }

    public ProjectReadModels.ProjectOrganizationReadModel toProjectOrganizationReadModel(ProjectOrganizationAggregate aggregate) {
        return new ProjectReadModels.ProjectOrganizationReadModel(
                aggregate.id(),
                aggregate.organizationId(),
                aggregate.roleType(),
                aggregate.joinedAt(),
                aggregate.active());
    }

    public ProjectViews.ProjectOrganizationView toOrganizationView(ProjectOrganizationAggregate aggregate) {
        return new ProjectViews.ProjectOrganizationView(
                aggregate.id(),
                aggregate.organizationId(),
                aggregate.roleType(),
                aggregate.joinedAt(),
                aggregate.active());
    }

    public ProjectReadModels.ProjectMemberReadModel toProjectMemberReadModel(ProjectMemberAggregate aggregate) {
        return new ProjectReadModels.ProjectMemberReadModel(
                aggregate.id(),
                aggregate.userId(),
                aggregate.organizationId(),
                aggregate.projectRole(),
                aggregate.active(),
                aggregate.assignedAt());
    }

    public ProjectViews.ProjectMemberView toMemberView(ProjectMemberAggregate aggregate) {
        return new ProjectViews.ProjectMemberView(
                aggregate.id(),
                aggregate.userId(),
                aggregate.organizationId(),
                aggregate.projectRole(),
                aggregate.active(),
                aggregate.assignedAt());
    }

    public ProjectReadModels.ProjectMilestoneListReadModel toProjectMilestoneListReadModel(ProjectMilestoneAggregate aggregate) {
        return new ProjectReadModels.ProjectMilestoneListReadModel(
                aggregate.id(),
                aggregate.structureNodeId(),
                aggregate.phaseId(),
                aggregate.code(),
                aggregate.name(),
                aggregate.sequence(),
                aggregate.plannedDate(),
                aggregate.actualDate(),
                aggregate.status(),
                aggregate.ownerOrganizationId(),
                aggregate.visibilityScope(),
                aggregate.version());
    }

    public ProjectViews.ProjectMilestoneView toMilestoneView(ProjectMilestoneAggregate aggregate) {
        return new ProjectViews.ProjectMilestoneView(
                aggregate.id(),
                aggregate.structureNodeId(),
                aggregate.phaseId(),
                aggregate.code(),
                aggregate.name(),
                aggregate.sequence(),
                aggregate.plannedDate(),
                aggregate.actualDate(),
                aggregate.status(),
                aggregate.ownerOrganizationId(),
                aggregate.visibilityScope(),
                aggregate.version());
    }

    public ProjectReadModels.ProjectDeliverableListReadModel toProjectDeliverableListReadModel(ProjectDeliverableAggregate aggregate) {
        return new ProjectReadModels.ProjectDeliverableListReadModel(
                aggregate.id(),
                aggregate.structureNodeId(),
                aggregate.phaseId(),
                aggregate.milestoneId(),
                aggregate.code(),
                aggregate.name(),
                aggregate.description(),
                aggregate.deliverableType(),
                aggregate.responsibleOrganizationId(),
                aggregate.responsibleUserId(),
                aggregate.approverOrganizationId(),
                aggregate.approverUserId(),
                aggregate.requiredDocument(),
                aggregate.plannedDueDate(),
                aggregate.submittedAt(),
                aggregate.approvedAt(),
                aggregate.status(),
                aggregate.priority(),
                aggregate.visibilityScope(),
                aggregate.version());
    }

    public ProjectReadModels.ProjectDeliverableDetailReadModel toProjectDeliverableDetailReadModel(ProjectDeliverableAggregate aggregate) {
        return new ProjectReadModels.ProjectDeliverableDetailReadModel(
                aggregate.id(),
                aggregate.structureNodeId(),
                aggregate.phaseId(),
                aggregate.milestoneId(),
                aggregate.code(),
                aggregate.name(),
                aggregate.description(),
                aggregate.deliverableType(),
                aggregate.responsibleOrganizationId(),
                aggregate.responsibleUserId(),
                aggregate.approverOrganizationId(),
                aggregate.approverUserId(),
                aggregate.requiredDocument(),
                aggregate.plannedDueDate(),
                aggregate.submittedAt(),
                aggregate.approvedAt(),
                aggregate.status(),
                aggregate.priority(),
                aggregate.visibilityScope(),
                aggregate.version());
    }

    public ProjectReadModels.PendingSubmissionReviewReadModel toPendingSubmissionReviewReadModel(ProjectDeliverableAggregate aggregate) {
        return new ProjectReadModels.PendingSubmissionReviewReadModel(
                aggregate.id(),
                aggregate.structureNodeId(),
                aggregate.phaseId(),
                aggregate.milestoneId(),
                aggregate.code(),
                aggregate.name(),
                aggregate.description(),
                aggregate.deliverableType(),
                aggregate.responsibleOrganizationId(),
                aggregate.responsibleUserId(),
                aggregate.approverOrganizationId(),
                aggregate.approverUserId(),
                aggregate.requiredDocument(),
                aggregate.plannedDueDate(),
                aggregate.submittedAt(),
                aggregate.approvedAt(),
                aggregate.status(),
                aggregate.priority(),
                aggregate.visibilityScope(),
                aggregate.version());
    }

    public ProjectViews.ProjectDeliverableView toDeliverableView(ProjectDeliverableAggregate aggregate) {
        return new ProjectViews.ProjectDeliverableView(
                aggregate.id(),
                aggregate.structureNodeId(),
                aggregate.phaseId(),
                aggregate.milestoneId(),
                aggregate.code(),
                aggregate.name(),
                aggregate.description(),
                aggregate.deliverableType(),
                aggregate.responsibleOrganizationId(),
                aggregate.responsibleUserId(),
                aggregate.approverOrganizationId(),
                aggregate.approverUserId(),
                aggregate.requiredDocument(),
                aggregate.plannedDueDate(),
                aggregate.submittedAt(),
                aggregate.approvedAt(),
                aggregate.status(),
                aggregate.priority(),
                aggregate.visibilityScope(),
                aggregate.version());
    }
}
