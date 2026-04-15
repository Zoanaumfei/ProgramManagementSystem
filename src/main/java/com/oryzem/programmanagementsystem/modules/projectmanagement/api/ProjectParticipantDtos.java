package com.oryzem.programmanagementsystem.modules.projectmanagement.api;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.ProjectViews;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.model.read.ProjectReadModels;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectMemberRole;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectOrganizationRoleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public final class ProjectParticipantDtos {

    private ProjectParticipantDtos() {
    }

    public record AddProjectOrganizationRequest(
            @NotBlank String organizationId,
            @NotNull ProjectOrganizationRoleType roleType) {
    }

    public record AddProjectMemberRequest(
            @NotBlank String userId,
            @NotBlank String organizationId,
            @NotNull ProjectMemberRole projectRole) {
    }

    public record ProjectOrganizationResponse(String id, String organizationId, ProjectOrganizationRoleType roleType, Instant joinedAt, boolean active) {
        static ProjectOrganizationResponse from(ProjectViews.ProjectOrganizationView view) {
            return new ProjectOrganizationResponse(view.id(), view.organizationId(), view.roleType(), view.joinedAt(), view.active());
        }

        static ProjectOrganizationResponse from(ProjectReadModels.ProjectOrganizationReadModel readModel) {
            return new ProjectOrganizationResponse(readModel.id(), readModel.organizationId(), readModel.roleType(), readModel.joinedAt(), readModel.active());
        }
    }

    public record ProjectMemberResponse(String id, String userId, String organizationId, ProjectMemberRole projectRole, boolean active, Instant assignedAt) {
        static ProjectMemberResponse from(ProjectViews.ProjectMemberView view) {
            return new ProjectMemberResponse(view.id(), view.userId(), view.organizationId(), view.projectRole(), view.active(), view.assignedAt());
        }

        static ProjectMemberResponse from(ProjectReadModels.ProjectMemberReadModel readModel) {
            return new ProjectMemberResponse(readModel.id(), readModel.userId(), readModel.organizationId(), readModel.projectRole(), readModel.active(), readModel.assignedAt());
        }
    }
}
