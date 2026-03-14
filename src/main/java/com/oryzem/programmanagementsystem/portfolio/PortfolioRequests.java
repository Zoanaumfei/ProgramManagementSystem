package com.oryzem.programmanagementsystem.portfolio;

import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

record CreateOrganizationRequest(
        @NotBlank String name,
        @NotBlank String code,
        OrganizationStatus status) {
}

record CreateMilestoneTemplateRequest(
        @NotBlank String name,
        String description,
        MilestoneTemplateStatus status,
        @Valid @NotEmpty List<CreateMilestoneTemplateItemRequest> items) {
}

record CreateMilestoneTemplateItemRequest(
        @NotBlank String name,
        @NotNull @PositiveOrZero Integer sortOrder,
        boolean required,
        @PositiveOrZero Integer offsetWeeks) {
}

record CreateProgramRequest(
        @NotBlank String name,
        @NotBlank String code,
        String description,
        @NotBlank String ownerOrganizationId,
        @NotNull @FutureOrPresent LocalDate plannedStartDate,
        @NotNull @FutureOrPresent LocalDate plannedEndDate,
        @Valid List<CreateProgramParticipationRequest> participants,
        @Valid @NotNull CreateProjectRequest initialProject) {
}

record CreateProgramParticipationRequest(
        @NotBlank String organizationId,
        @NotNull ParticipationRole role,
        ParticipationStatus status) {
}

record CreateProjectRequest(
        @NotBlank String name,
        @NotBlank String code,
        String description,
        @NotNull @FutureOrPresent LocalDate plannedStartDate,
        @NotNull @FutureOrPresent LocalDate plannedEndDate,
        String milestoneTemplateId,
        ProjectStatus status) {
}

record CreateProductRequest(
        @NotBlank String name,
        @NotBlank String code,
        String description,
        ProductStatus status) {
}

record CreateItemRequest(
        @NotBlank String name,
        @NotBlank String code,
        String description,
        ItemStatus status) {
}

record CreateDeliverableRequest(
        @NotBlank String name,
        String description,
        @NotNull DeliverableType type,
        DeliverableStatus status,
        @NotNull @FutureOrPresent LocalDate plannedDate,
        @NotNull @FutureOrPresent LocalDate dueDate) {
}

record CreateOpenIssueRequest(
        @NotBlank String title,
        String description,
        @NotNull OpenIssueSeverity severity,
        OpenIssueStatus status,
        OffsetDateTime openedAt) {
}

record PrepareDeliverableDocumentUploadRequest(
        @NotBlank String fileName,
        @NotBlank String contentType,
        @NotNull @Positive Long fileSize) {
}
