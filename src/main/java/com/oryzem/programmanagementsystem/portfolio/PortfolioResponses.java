package com.oryzem.programmanagementsystem.portfolio;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

record OrganizationResponse(
        String id,
        String name,
        String code,
        OrganizationStatus status,
        OrganizationSetupStatus setupStatus,
        Instant createdAt,
        Instant updatedAt) {

    static OrganizationResponse from(OrganizationEntity organization, OrganizationSetupStatus setupStatus) {
        return new OrganizationResponse(
                organization.getId(),
                organization.getName(),
                organization.getCode(),
                organization.getStatus(),
                setupStatus,
                organization.getCreatedAt(),
                organization.getUpdatedAt());
    }
}

record MilestoneTemplateResponse(
        String id,
        String name,
        String description,
        MilestoneTemplateStatus status,
        List<MilestoneTemplateItemResponse> items,
        Instant createdAt,
        Instant updatedAt) {

    static MilestoneTemplateResponse from(MilestoneTemplateEntity template) {
        return new MilestoneTemplateResponse(
                template.getId(),
                template.getName(),
                template.getDescription(),
                template.getStatus(),
                template.getItems().stream()
                        .sorted(Comparator.comparing(MilestoneTemplateItemEntity::getSortOrder))
                        .map(MilestoneTemplateItemResponse::from)
                        .toList(),
                template.getCreatedAt(),
                template.getUpdatedAt());
    }
}

record MilestoneTemplateItemResponse(
        String id,
        String name,
        int sortOrder,
        boolean required,
        Integer offsetWeeks) {

    static MilestoneTemplateItemResponse from(MilestoneTemplateItemEntity item) {
        return new MilestoneTemplateItemResponse(
                item.getId(),
                item.getName(),
                item.getSortOrder(),
                item.isRequired(),
                item.getOffsetWeeks());
    }
}

record ProgramSummaryResponse(
        String id,
        String name,
        String code,
        ProgramStatus status,
        String ownerOrganizationId,
        LocalDate plannedStartDate,
        LocalDate plannedEndDate,
        int projectCount,
        int participantCount,
        int openIssueCount,
        Instant createdAt,
        Instant updatedAt) {

    static ProgramSummaryResponse from(ProgramEntity program) {
        return new ProgramSummaryResponse(
                program.getId(),
                program.getName(),
                program.getCode(),
                program.getStatus(),
                program.getOwnerOrganization().getId(),
                program.getPlannedStartDate(),
                program.getPlannedEndDate(),
                program.getProjects().size(),
                program.getParticipants().size(),
                program.getOpenIssues().size(),
                program.getCreatedAt(),
                program.getUpdatedAt());
    }
}

record ProgramDetailResponse(
        String id,
        String name,
        String code,
        String description,
        ProgramStatus status,
        String ownerOrganizationId,
        LocalDate plannedStartDate,
        LocalDate plannedEndDate,
        List<ProgramParticipationResponse> participants,
        List<ProjectResponse> projects,
        List<OpenIssueResponse> openIssues,
        Instant createdAt,
        Instant updatedAt) {

    static ProgramDetailResponse from(ProgramEntity program) {
        return new ProgramDetailResponse(
                program.getId(),
                program.getName(),
                program.getCode(),
                program.getDescription(),
                program.getStatus(),
                program.getOwnerOrganization().getId(),
                program.getPlannedStartDate(),
                program.getPlannedEndDate(),
                program.getParticipants().stream()
                        .sorted(Comparator.comparing(participation -> participation.getOrganization().getName()))
                        .map(ProgramParticipationResponse::from)
                        .toList(),
                program.getProjects().stream()
                        .sorted(Comparator.comparing(ProjectEntity::getCreatedAt))
                        .map(ProjectResponse::from)
                        .toList(),
                program.getOpenIssues().stream()
                        .sorted(Comparator.comparing(OpenIssueEntity::getOpenedAt))
                        .map(OpenIssueResponse::from)
                        .toList(),
                program.getCreatedAt(),
                program.getUpdatedAt());
    }
}

record ProgramParticipationResponse(
        String id,
        String organizationId,
        String organizationName,
        ParticipationRole role,
        ParticipationStatus status) {

    static ProgramParticipationResponse from(ProgramParticipationEntity participation) {
        return new ProgramParticipationResponse(
                participation.getId(),
                participation.getOrganization().getId(),
                participation.getOrganization().getName(),
                participation.getRole(),
                participation.getStatus());
    }
}

record ProjectResponse(
        String id,
        String programId,
        String name,
        String code,
        String description,
        ProjectStatus status,
        LocalDate plannedStartDate,
        LocalDate plannedEndDate,
        String appliedMilestoneTemplateId,
        List<ProjectMilestoneResponse> milestones,
        List<ProductResponse> products,
        Instant createdAt,
        Instant updatedAt) {

    static ProjectResponse from(ProjectEntity project) {
        return new ProjectResponse(
                project.getId(),
                project.getProgram().getId(),
                project.getName(),
                project.getCode(),
                project.getDescription(),
                project.getStatus(),
                project.getPlannedStartDate(),
                project.getPlannedEndDate(),
                project.getAppliedMilestoneTemplateId(),
                project.getMilestones().stream()
                        .sorted(Comparator.comparing(ProjectMilestoneEntity::getSortOrder))
                        .map(ProjectMilestoneResponse::from)
                        .toList(),
                project.getProducts().stream()
                        .sorted(Comparator.comparing(ProductEntity::getCreatedAt))
                        .map(ProductResponse::from)
                        .toList(),
                project.getCreatedAt(),
                project.getUpdatedAt());
    }
}

record ProjectMilestoneResponse(
        String id,
        String name,
        int sortOrder,
        ProjectMilestoneStatus status,
        LocalDate plannedDate,
        LocalDate actualDate,
        String milestoneTemplateItemId) {

    static ProjectMilestoneResponse from(ProjectMilestoneEntity milestone) {
        return new ProjectMilestoneResponse(
                milestone.getId(),
                milestone.getName(),
                milestone.getSortOrder(),
                milestone.getStatus(),
                milestone.getPlannedDate(),
                milestone.getActualDate(),
                milestone.getMilestoneTemplateItemId());
    }
}

record ProductResponse(
        String id,
        String projectId,
        String name,
        String code,
        String description,
        ProductStatus status,
        List<ItemResponse> items,
        Instant createdAt,
        Instant updatedAt) {

    static ProductResponse from(ProductEntity product) {
        return new ProductResponse(
                product.getId(),
                product.getProject().getId(),
                product.getName(),
                product.getCode(),
                product.getDescription(),
                product.getStatus(),
                product.getItems().stream()
                        .sorted(Comparator.comparing(ItemEntity::getCreatedAt))
                        .map(ItemResponse::from)
                        .toList(),
                product.getCreatedAt(),
                product.getUpdatedAt());
    }
}

record ItemResponse(
        String id,
        String productId,
        String name,
        String code,
        String description,
        ItemStatus status,
        List<DeliverableResponse> deliverables,
        Instant createdAt,
        Instant updatedAt) {

    static ItemResponse from(ItemEntity item) {
        return new ItemResponse(
                item.getId(),
                item.getProduct().getId(),
                item.getName(),
                item.getCode(),
                item.getDescription(),
                item.getStatus(),
                item.getDeliverables().stream()
                        .sorted(Comparator.comparing(DeliverableEntity::getCreatedAt))
                        .map(DeliverableResponse::from)
                        .toList(),
                item.getCreatedAt(),
                item.getUpdatedAt());
    }
}

record DeliverableResponse(
        String id,
        String itemId,
        String name,
        String description,
        DeliverableType type,
        DeliverableStatus status,
        LocalDate plannedDate,
        LocalDate dueDate,
        OffsetDateTime submittedAt,
        OffsetDateTime approvedAt,
        OffsetDateTime completedAt,
        List<DeliverableDocumentResponse> documents,
        Instant createdAt,
        Instant updatedAt) {

    static DeliverableResponse from(DeliverableEntity deliverable) {
        return new DeliverableResponse(
                deliverable.getId(),
                deliverable.getItem().getId(),
                deliverable.getName(),
                deliverable.getDescription(),
                deliverable.getType(),
                deliverable.getStatus(),
                deliverable.getPlannedDate(),
                deliverable.getDueDate(),
                deliverable.getSubmittedAt(),
                deliverable.getApprovedAt(),
                deliverable.getCompletedAt(),
                deliverable.getDocuments().stream()
                        .sorted(Comparator.comparing(DeliverableDocumentEntity::getCreatedAt))
                        .map(DeliverableDocumentResponse::from)
                        .toList(),
                deliverable.getCreatedAt(),
                deliverable.getUpdatedAt());
    }
}

record DeliverableDocumentResponse(
        String id,
        String deliverableId,
        String fileName,
        String contentType,
        long fileSize,
        DeliverableDocumentStatus status,
        OffsetDateTime uploadedAt,
        Instant createdAt,
        Instant updatedAt) {

    static DeliverableDocumentResponse from(DeliverableDocumentEntity document) {
        return new DeliverableDocumentResponse(
                document.getId(),
                document.getDeliverable().getId(),
                document.getFileName(),
                document.getContentType(),
                document.getFileSize(),
                document.getStatus(),
                document.getUploadedAt(),
                document.getCreatedAt(),
                document.getUpdatedAt());
    }
}

record DeliverableDocumentUploadResponse(
        DeliverableDocumentResponse document,
        String uploadUrl,
        Instant expiresAt,
        Map<String, String> requiredHeaders) {
}

record DeliverableDocumentDownloadResponse(
        String documentId,
        String downloadUrl,
        Instant expiresAt) {
}

record OpenIssueResponse(
        String id,
        String programId,
        String title,
        String description,
        OpenIssueStatus status,
        OpenIssueSeverity severity,
        OffsetDateTime openedAt,
        OffsetDateTime resolvedAt,
        OffsetDateTime closedAt,
        Instant createdAt,
        Instant updatedAt) {

    static OpenIssueResponse from(OpenIssueEntity openIssue) {
        return new OpenIssueResponse(
                openIssue.getId(),
                openIssue.getProgram().getId(),
                openIssue.getTitle(),
                openIssue.getDescription(),
                openIssue.getStatus(),
                openIssue.getSeverity(),
                openIssue.getOpenedAt(),
                openIssue.getResolvedAt(),
                openIssue.getClosedAt(),
                openIssue.getCreatedAt(),
                openIssue.getUpdatedAt());
    }
}
