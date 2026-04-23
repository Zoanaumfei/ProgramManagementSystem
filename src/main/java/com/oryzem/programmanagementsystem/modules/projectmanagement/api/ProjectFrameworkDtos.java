package com.oryzem.programmanagementsystem.modules.projectmanagement.api;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.ProjectFrameworkViews;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectFrameworkUiLayout;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public final class ProjectFrameworkDtos {

    private ProjectFrameworkDtos() {
    }

    public record CreateProjectFrameworkRequest(
            @NotBlank String code,
            @NotBlank String displayName,
            String description,
            @NotNull ProjectFrameworkUiLayout uiLayout,
            boolean active) {
    }

    public record UpdateProjectFrameworkRequest(
            @NotBlank String displayName,
            String description,
            @NotNull ProjectFrameworkUiLayout uiLayout,
            boolean active) {
    }

    public record ProjectFrameworkResponse(
            String id,
            String code,
            String displayName,
            String description,
            ProjectFrameworkUiLayout uiLayout,
            boolean active,
            Instant createdAt,
            Instant updatedAt) {
        static ProjectFrameworkResponse from(ProjectFrameworkViews.ProjectFrameworkView view) {
            return new ProjectFrameworkResponse(
                    view.id(),
                    view.code(),
                    view.displayName(),
                    view.description(),
                    view.uiLayout(),
                    view.active(),
                    view.createdAt(),
                    view.updatedAt());
        }
    }
}
