package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.DeliverableType;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectDeliverableStatus;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectMemberRole;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectMilestoneStatus;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectOrganizationRoleType;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectPriority;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectStatus;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectVisibilityScope;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class ProjectViews {

    private ProjectViews() {
    }

    public record ProjectSummaryView(
            String id,
            String code,
            String name,
            String frameworkType,
            ProjectStatus status,
            ProjectVisibilityScope visibilityScope,
            String leadOrganizationId,
            String customerOrganizationId,
            LocalDate plannedStartDate,
            LocalDate plannedEndDate,
            Instant createdAt) {
    }

    public record ProjectDetailView(
            String id,
            String code,
            String name,
            String description,
            String frameworkType,
            String templateId,
            int templateVersion,
            String leadOrganizationId,
            String customerOrganizationId,
            ProjectStatus status,
            ProjectVisibilityScope visibilityScope,
            LocalDate plannedStartDate,
            LocalDate plannedEndDate,
            LocalDate actualStartDate,
            LocalDate actualEndDate,
            String createdByUserId,
            Instant createdAt,
            Instant updatedAt,
            long version,
            List<ProjectOrganizationView> organizations,
            List<ProjectMemberView> members) {
    }

    public record ProjectOrganizationView(
            String id,
            String organizationId,
            ProjectOrganizationRoleType roleType,
            Instant joinedAt,
            boolean active) {
    }

    public record ProjectMemberView(
            String id,
            String userId,
            String organizationId,
            ProjectMemberRole projectRole,
            boolean active,
            Instant assignedAt) {
    }

    public record ProjectMilestoneView(
            String id,
            String structureNodeId,
            String phaseId,
            String code,
            String name,
            int sequence,
            LocalDate plannedDate,
            LocalDate actualDate,
            ProjectMilestoneStatus status,
            String ownerOrganizationId,
            ProjectVisibilityScope visibilityScope,
            long version) {
    }

    public record ProjectDeliverableView(
            String id,
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
    }
}
