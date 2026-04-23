package com.oryzem.programmanagementsystem.modules.projectmanagement.api;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.ProjectViews;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.model.read.ProjectReadModels;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectStatus;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectVisibilityScope;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class ProjectDtos {

    private ProjectDtos() {
    }

    public record CreateProjectRequest(
            @NotBlank String code,
            @NotBlank String name,
            String description,
            @NotBlank String frameworkType,
            String templateId,
            String customerOrganizationId,
            ProjectStatus status,
            ProjectVisibilityScope visibilityScope,
            LocalDate plannedStartDate,
            LocalDate plannedEndDate) {
    }

    public record UpdateProjectRequest(
            @NotBlank String name,
            String description,
            ProjectVisibilityScope visibilityScope,
            LocalDate plannedStartDate,
            LocalDate plannedEndDate,
            ProjectStatus status,
            long version) {
    }

    public record ProjectSummaryResponse(
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
        static ProjectSummaryResponse from(ProjectViews.ProjectSummaryView view) {
            return new ProjectSummaryResponse(view.id(), view.code(), view.name(), view.frameworkType(), view.status(), view.visibilityScope(), view.leadOrganizationId(), view.customerOrganizationId(), view.plannedStartDate(), view.plannedEndDate(), view.createdAt());
        }

        static ProjectSummaryResponse from(ProjectReadModels.ProjectListReadModel readModel) {
            return new ProjectSummaryResponse(readModel.id(), readModel.code(), readModel.name(), readModel.frameworkType(), readModel.status(), readModel.visibilityScope(), readModel.leadOrganizationId(), readModel.customerOrganizationId(), readModel.plannedStartDate(), readModel.plannedEndDate(), readModel.createdAt());
        }
    }

    public record ProjectDetailResponse(
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
            List<ProjectParticipantDtos.ProjectOrganizationResponse> organizations,
            List<ProjectParticipantDtos.ProjectMemberResponse> members) {
        static ProjectDetailResponse from(ProjectViews.ProjectDetailView view) {
            return new ProjectDetailResponse(view.id(), view.code(), view.name(), view.description(), view.frameworkType(), view.templateId(), view.templateVersion(), view.leadOrganizationId(), view.customerOrganizationId(), view.status(), view.visibilityScope(), view.plannedStartDate(), view.plannedEndDate(), view.actualStartDate(), view.actualEndDate(), view.createdByUserId(), view.createdAt(), view.updatedAt(), view.version(), view.organizations().stream().map(ProjectParticipantDtos.ProjectOrganizationResponse::from).toList(), view.members().stream().map(ProjectParticipantDtos.ProjectMemberResponse::from).toList());
        }

        static ProjectDetailResponse from(ProjectReadModels.ProjectDetailReadModel readModel) {
            return new ProjectDetailResponse(readModel.id(), readModel.code(), readModel.name(), readModel.description(), readModel.frameworkType(), readModel.templateId(), readModel.templateVersion(), readModel.leadOrganizationId(), readModel.customerOrganizationId(), readModel.status(), readModel.visibilityScope(), readModel.plannedStartDate(), readModel.plannedEndDate(), readModel.actualStartDate(), readModel.actualEndDate(), readModel.createdByUserId(), readModel.createdAt(), readModel.updatedAt(), readModel.version(), readModel.organizations().stream().map(ProjectParticipantDtos.ProjectOrganizationResponse::from).toList(), readModel.members().stream().map(ProjectParticipantDtos.ProjectMemberResponse::from).toList());
        }
    }
}
