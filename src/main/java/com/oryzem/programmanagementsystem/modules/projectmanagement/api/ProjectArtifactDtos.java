package com.oryzem.programmanagementsystem.modules.projectmanagement.api;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.ProjectViews;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.model.read.ProjectReadModels;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.DeliverableType;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectDeliverableStatus;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectMilestoneStatus;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectPriority;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectVisibilityScope;
import java.time.Instant;
import java.time.LocalDate;

public final class ProjectArtifactDtos {

    private ProjectArtifactDtos() {
    }

    public record UpdateMilestoneRequest(
            String code,
            String name,
            LocalDate plannedDate,
            LocalDate actualDate,
            ProjectMilestoneStatus status,
            String ownerOrganizationId,
            ProjectVisibilityScope visibilityScope,
            long version) {
    }

    public record CreateMilestoneRequest(
            String structureNodeId,
            String phaseId,
            String code,
            String name,
            LocalDate plannedDate,
            String ownerOrganizationId,
            ProjectVisibilityScope visibilityScope) {
    }

    public record UpdateDeliverableRequest(
            String code,
            String name,
            String description,
            DeliverableType deliverableType,
            String responsibleOrganizationId,
            String responsibleUserId,
            String approverOrganizationId,
            String approverUserId,
            Boolean requiredDocument,
            LocalDate plannedDueDate,
            ProjectDeliverableStatus status,
            ProjectPriority priority,
            ProjectVisibilityScope visibilityScope,
            long version) {
    }

    public record CreateDeliverableRequest(
            String structureNodeId,
            String phaseId,
            String milestoneId,
            String code,
            String name,
            String description,
            DeliverableType deliverableType,
            String responsibleOrganizationId,
            String responsibleUserId,
            String approverOrganizationId,
            String approverUserId,
            Boolean requiredDocument,
            LocalDate plannedDueDate,
            ProjectPriority priority,
            ProjectVisibilityScope visibilityScope) {
    }

    public record ProjectMilestoneResponse(String id, String structureNodeId, String phaseId, String code, String name, int sequence, LocalDate plannedDate, LocalDate actualDate, ProjectMilestoneStatus status, String ownerOrganizationId, ProjectVisibilityScope visibilityScope, long version) {
        static ProjectMilestoneResponse from(ProjectViews.ProjectMilestoneView view) {
            return new ProjectMilestoneResponse(view.id(), view.structureNodeId(), view.phaseId(), view.code(), view.name(), view.sequence(), view.plannedDate(), view.actualDate(), view.status(), view.ownerOrganizationId(), view.visibilityScope(), view.version());
        }

        static ProjectMilestoneResponse from(ProjectReadModels.ProjectMilestoneListReadModel readModel) {
            return new ProjectMilestoneResponse(readModel.id(), readModel.structureNodeId(), readModel.phaseId(), readModel.code(), readModel.name(), readModel.sequence(), readModel.plannedDate(), readModel.actualDate(), readModel.status(), readModel.ownerOrganizationId(), readModel.visibilityScope(), readModel.version());
        }
    }

    public record ProjectDeliverableResponse(String id, String structureNodeId, String phaseId, String milestoneId, String code, String name, String description, DeliverableType deliverableType, String responsibleOrganizationId, String responsibleUserId, String approverOrganizationId, String approverUserId, boolean requiredDocument, LocalDate plannedDueDate, Instant submittedAt, Instant approvedAt, ProjectDeliverableStatus status, ProjectPriority priority, ProjectVisibilityScope visibilityScope, long version) {
        static ProjectDeliverableResponse from(ProjectViews.ProjectDeliverableView view) {
            return new ProjectDeliverableResponse(view.id(), view.structureNodeId(), view.phaseId(), view.milestoneId(), view.code(), view.name(), view.description(), view.deliverableType(), view.responsibleOrganizationId(), view.responsibleUserId(), view.approverOrganizationId(), view.approverUserId(), view.requiredDocument(), view.plannedDueDate(), view.submittedAt(), view.approvedAt(), view.status(), view.priority(), view.visibilityScope(), view.version());
        }

        static ProjectDeliverableResponse from(ProjectReadModels.ProjectDeliverableListReadModel readModel) {
            return new ProjectDeliverableResponse(readModel.id(), readModel.structureNodeId(), readModel.phaseId(), readModel.milestoneId(), readModel.code(), readModel.name(), readModel.description(), readModel.deliverableType(), readModel.responsibleOrganizationId(), readModel.responsibleUserId(), readModel.approverOrganizationId(), readModel.approverUserId(), readModel.requiredDocument(), readModel.plannedDueDate(), readModel.submittedAt(), readModel.approvedAt(), readModel.status(), readModel.priority(), readModel.visibilityScope(), readModel.version());
        }

        static ProjectDeliverableResponse from(ProjectReadModels.ProjectDeliverableDetailReadModel readModel) {
            return new ProjectDeliverableResponse(readModel.id(), readModel.structureNodeId(), readModel.phaseId(), readModel.milestoneId(), readModel.code(), readModel.name(), readModel.description(), readModel.deliverableType(), readModel.responsibleOrganizationId(), readModel.responsibleUserId(), readModel.approverOrganizationId(), readModel.approverUserId(), readModel.requiredDocument(), readModel.plannedDueDate(), readModel.submittedAt(), readModel.approvedAt(), readModel.status(), readModel.priority(), readModel.visibilityScope(), readModel.version());
        }

        static ProjectDeliverableResponse from(ProjectReadModels.PendingSubmissionReviewReadModel readModel) {
            return new ProjectDeliverableResponse(readModel.id(), readModel.structureNodeId(), readModel.phaseId(), readModel.milestoneId(), readModel.code(), readModel.name(), readModel.description(), readModel.deliverableType(), readModel.responsibleOrganizationId(), readModel.responsibleUserId(), readModel.approverOrganizationId(), readModel.approverUserId(), readModel.requiredDocument(), readModel.plannedDueDate(), readModel.submittedAt(), readModel.approvedAt(), readModel.status(), readModel.priority(), readModel.visibilityScope(), readModel.version());
        }
    }
}
