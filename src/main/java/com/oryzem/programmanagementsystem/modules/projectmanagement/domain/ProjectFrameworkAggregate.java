package com.oryzem.programmanagementsystem.modules.projectmanagement.domain;

import com.oryzem.programmanagementsystem.platform.shared.BusinessRuleException;
import java.time.Instant;

public record ProjectFrameworkAggregate(
        String id,
        String code,
        String displayName,
        String description,
        ProjectFrameworkUiLayout uiLayout,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {

    public ProjectFrameworkAggregate {
        if (code == null || code.isBlank()) {
            throw new BusinessRuleException("PROJECT_FRAMEWORK_CODE_REQUIRED", "Project framework code is required.");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new BusinessRuleException("PROJECT_FRAMEWORK_DISPLAY_NAME_REQUIRED", "Project framework display name is required.");
        }
        if (uiLayout == null) {
            throw new BusinessRuleException("PROJECT_FRAMEWORK_UI_LAYOUT_REQUIRED", "Project framework UI layout is required.");
        }
    }

    public ProjectFrameworkAggregate update(String displayName, String description, ProjectFrameworkUiLayout uiLayout, boolean active, Instant now) {
        return new ProjectFrameworkAggregate(
                id,
                code,
                requireText(displayName, "PROJECT_FRAMEWORK_DISPLAY_NAME_REQUIRED", "Project framework display name is required."),
                normalize(description),
                uiLayout,
                active,
                createdAt,
                now);
    }

    private static String requireText(String value, String code, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessRuleException(code, message);
        }
        return value.trim();
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
