package com.oryzem.programmanagementsystem.modules.projectmanagement;

import java.time.LocalDate;

final class ProjectManagementValidationSupport {

    private ProjectManagementValidationSupport() {
    }

    static void validateDateRange(LocalDate startDate, LocalDate endDate, String resourceName) {
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException(resourceName + " end date cannot be before start date.");
        }
    }

    static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    static <T> T defaultValue(T value, T fallback) {
        return value != null ? value : fallback;
    }
}
