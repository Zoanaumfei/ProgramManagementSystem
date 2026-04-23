package com.oryzem.programmanagementsystem.modules.projectmanagement.domain;

import com.oryzem.programmanagementsystem.platform.shared.BusinessRuleException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Map;

public record ProjectAggregate(
        String id,
        String tenantId,
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
        long version) {

    public ProjectAggregate transitionTo(ProjectStatus nextStatus, Instant now, LocalDate today) {
        if (nextStatus == null || nextStatus == status) {
            return this;
        }
        if (!isAllowedTransition(status, nextStatus)) {
            throw new BusinessRuleException(
                    "PROJECT_STATUS_TRANSITION_NOT_ALLOWED",
                    "Project status transition is not allowed.",
                    Map.of("currentStatus", status.name(), "nextStatus", nextStatus.name()));
        }
        LocalDate nextActualStart = actualStartDate;
        LocalDate nextActualEnd = actualEndDate;
        if (nextStatus == ProjectStatus.ACTIVE && nextActualStart == null) {
            nextActualStart = today;
        }
        if (EnumSet.of(ProjectStatus.COMPLETED, ProjectStatus.CANCELLED).contains(nextStatus) && nextActualEnd == null) {
            nextActualEnd = today;
        }
        return new ProjectAggregate(
                id,
                tenantId,
                code,
                name,
                description,
                frameworkType,
                templateId,
                templateVersion,
                leadOrganizationId,
                customerOrganizationId,
                nextStatus,
                visibilityScope,
                plannedStartDate,
                plannedEndDate,
                nextActualStart,
                nextActualEnd,
                createdByUserId,
                createdAt,
                now,
                version);
    }

    public ProjectAggregate update(
            String nextName,
            String nextDescription,
            ProjectVisibilityScope nextVisibilityScope,
            LocalDate nextPlannedStartDate,
            LocalDate nextPlannedEndDate,
            ProjectStatus nextStatus,
            Instant now,
            LocalDate today) {
        assertMutable();
        if (nextPlannedStartDate != null && nextPlannedEndDate != null && nextPlannedEndDate.isBefore(nextPlannedStartDate)) {
            throw new BusinessRuleException(
                    "PROJECT_DATE_RANGE_INVALID",
                    "Project planned end date cannot be before planned start date.");
        }
        ProjectAggregate updated = new ProjectAggregate(
                id,
                tenantId,
                code,
                requireText(nextName, "name"),
                nextDescription,
                frameworkType,
                templateId,
                templateVersion,
                leadOrganizationId,
                customerOrganizationId,
                status,
                nextVisibilityScope != null ? nextVisibilityScope : visibilityScope,
                nextPlannedStartDate,
                nextPlannedEndDate,
                actualStartDate,
                actualEndDate,
                createdByUserId,
                createdAt,
                now,
                version);
        return nextStatus != null ? updated.transitionTo(nextStatus, now, today) : updated;
    }

    public void assertMutable() {
        if (status == ProjectStatus.COMPLETED || status == ProjectStatus.CANCELLED) {
            throw new BusinessRuleException(
                    "PROJECT_NOT_MUTABLE",
                    "Completed or cancelled projects cannot be changed.",
                    Map.of("status", status.name()));
        }
    }

    private boolean isAllowedTransition(ProjectStatus current, ProjectStatus next) {
        return switch (current) {
            case DRAFT -> next == ProjectStatus.PLANNED || next == ProjectStatus.CANCELLED;
            case PLANNED -> next == ProjectStatus.ACTIVE || next == ProjectStatus.CANCELLED;
            case ACTIVE -> next == ProjectStatus.ON_HOLD || next == ProjectStatus.COMPLETED || next == ProjectStatus.CANCELLED;
            case ON_HOLD -> next == ProjectStatus.ACTIVE || next == ProjectStatus.CANCELLED;
            case COMPLETED, CANCELLED -> false;
        };
    }

    private String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new BusinessRuleException(
                    "PROJECT_FIELD_REQUIRED",
                    "Project field is required.",
                    Map.of("field", field));
        }
        return value.trim();
    }
}
