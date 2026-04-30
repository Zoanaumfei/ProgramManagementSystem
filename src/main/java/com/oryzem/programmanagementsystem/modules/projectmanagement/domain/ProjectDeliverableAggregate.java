package com.oryzem.programmanagementsystem.modules.projectmanagement.domain;

import com.oryzem.programmanagementsystem.platform.shared.BusinessRuleException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Map;

public record ProjectDeliverableAggregate(
        String id,
        String projectId,
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
        boolean requiredDocument,
        LocalDate plannedDueDate,
        Instant submittedAt,
        Instant approvedAt,
        ProjectDeliverableStatus status,
        ProjectPriority priority,
        ProjectVisibilityScope visibilityScope,
        long version) {

    public ProjectDeliverableAggregate updateOperationalFields(
            String nextCode,
            String nextName,
            String nextDescription,
            DeliverableType nextDeliverableType,
            String nextResponsibleOrganizationId,
            String nextResponsibleUserId,
            String nextApproverOrganizationId,
            String nextApproverUserId,
            boolean nextRequiredDocument,
            LocalDate nextPlannedDueDate,
            ProjectDeliverableStatus nextStatus,
            ProjectPriority nextPriority,
            ProjectVisibilityScope nextVisibilityScope) {
        if (nextStatus != null && EnumSet.of(
                ProjectDeliverableStatus.SUBMITTED,
                ProjectDeliverableStatus.UNDER_REVIEW,
                ProjectDeliverableStatus.APPROVED,
                ProjectDeliverableStatus.REJECTED).contains(nextStatus)) {
            throw new BusinessRuleException(
                    "DELIVERABLE_STATUS_RESTRICTED",
                    "Deliverable review statuses must be changed through the submission workflow.");
        }
        return new ProjectDeliverableAggregate(
                id,
                projectId,
                structureNodeId,
                phaseId,
                milestoneId,
                requireText(nextCode, "code"),
                requireText(nextName, "name"),
                nextDescription,
                nextDeliverableType != null ? nextDeliverableType : deliverableType,
                nextResponsibleOrganizationId,
                nextResponsibleUserId,
                nextApproverOrganizationId,
                nextApproverUserId,
                nextRequiredDocument,
                nextPlannedDueDate,
                submittedAt,
                approvedAt,
                nextStatus != null ? nextStatus : status,
                nextPriority != null ? nextPriority : priority,
                nextVisibilityScope != null ? nextVisibilityScope : visibilityScope,
                version);
    }

    private String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new BusinessRuleException(
                    "PROJECT_DELIVERABLE_FIELD_REQUIRED",
                    "Project deliverable field is required.",
                    Map.of("field", field));
        }
        return value.trim();
    }

    public void assertCanSubmit() {
        if (status == ProjectDeliverableStatus.APPROVED || status == ProjectDeliverableStatus.WAIVED) {
            throw new BusinessRuleException(
                    "DELIVERABLE_SUBMISSION_NOT_ALLOWED",
                    "This deliverable cannot receive a new submission in its current status.",
                    Map.of("status", status.name()));
        }
        if (responsibleOrganizationId == null || responsibleOrganizationId.isBlank()) {
            throw new BusinessRuleException(
                    "DELIVERABLE_RESPONSIBLE_ORGANIZATION_REQUIRED",
                    "A responsible organization is required before submission.");
        }
    }

    public ProjectDeliverableAggregate markSubmitted(Instant when) {
        assertCanSubmit();
        return new ProjectDeliverableAggregate(
                id,
                projectId,
                structureNodeId,
                phaseId,
                milestoneId,
                code,
                name,
                description,
                deliverableType,
                responsibleOrganizationId,
                responsibleUserId,
                approverOrganizationId,
                approverUserId,
                requiredDocument,
                plannedDueDate,
                when,
                approvedAt,
                ProjectDeliverableStatus.SUBMITTED,
                priority,
                visibilityScope,
                version);
    }

    public ProjectDeliverableAggregate markUnderReview() {
        if (status != ProjectDeliverableStatus.SUBMITTED && status != ProjectDeliverableStatus.UNDER_REVIEW) {
            throw new BusinessRuleException(
                    "DELIVERABLE_REVIEW_NOT_ALLOWED",
                    "Only submitted deliverables can enter review.",
                    Map.of("status", status.name()));
        }
        return new ProjectDeliverableAggregate(
                id,
                projectId,
                structureNodeId,
                phaseId,
                milestoneId,
                code,
                name,
                description,
                deliverableType,
                responsibleOrganizationId,
                responsibleUserId,
                approverOrganizationId,
                approverUserId,
                requiredDocument,
                plannedDueDate,
                submittedAt,
                approvedAt,
                ProjectDeliverableStatus.UNDER_REVIEW,
                priority,
                visibilityScope,
                version);
    }

    public ProjectDeliverableAggregate markApproved(Instant when) {
        if (status != ProjectDeliverableStatus.SUBMITTED && status != ProjectDeliverableStatus.UNDER_REVIEW) {
            throw new BusinessRuleException(
                    "DELIVERABLE_APPROVAL_NOT_ALLOWED",
                    "Only submitted deliverables can be approved.",
                    Map.of("status", status.name()));
        }
        return new ProjectDeliverableAggregate(
                id,
                projectId,
                structureNodeId,
                phaseId,
                milestoneId,
                code,
                name,
                description,
                deliverableType,
                responsibleOrganizationId,
                responsibleUserId,
                approverOrganizationId,
                approverUserId,
                requiredDocument,
                plannedDueDate,
                submittedAt,
                when,
                ProjectDeliverableStatus.APPROVED,
                priority,
                visibilityScope,
                version);
    }

    public ProjectDeliverableAggregate markRejected() {
        if (status != ProjectDeliverableStatus.SUBMITTED && status != ProjectDeliverableStatus.UNDER_REVIEW) {
            throw new BusinessRuleException(
                    "DELIVERABLE_REJECTION_NOT_ALLOWED",
                    "Only submitted deliverables can be rejected.",
                    Map.of("status", status.name()));
        }
        return new ProjectDeliverableAggregate(
                id,
                projectId,
                structureNodeId,
                phaseId,
                milestoneId,
                code,
                name,
                description,
                deliverableType,
                responsibleOrganizationId,
                responsibleUserId,
                approverOrganizationId,
                approverUserId,
                requiredDocument,
                plannedDueDate,
                submittedAt,
                null,
                ProjectDeliverableStatus.REJECTED,
                priority,
                visibilityScope,
                version);
    }
}
