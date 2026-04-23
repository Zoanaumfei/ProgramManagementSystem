package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectFrameworkUiLayout;
import java.time.Instant;

public final class ProjectFrameworkViews {

    private ProjectFrameworkViews() {
    }

    public record ProjectFrameworkView(
            String id,
            String code,
            String displayName,
            String description,
            ProjectFrameworkUiLayout uiLayout,
            boolean active,
            Instant createdAt,
            Instant updatedAt) {
    }
}
