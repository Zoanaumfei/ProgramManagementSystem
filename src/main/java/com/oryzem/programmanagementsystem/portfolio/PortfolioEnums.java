package com.oryzem.programmanagementsystem.portfolio;

enum OrganizationStatus {
    ACTIVE,
    INACTIVE
}

enum OrganizationSetupStatus {
    COMPLETED,
    INCOMPLETED
}

enum ProgramStatus {
    DRAFT,
    ACTIVE,
    PAUSED,
    CLOSED,
    CANCELED
}

enum ParticipationRole {
    CLIENT,
    SUPPLIER,
    INTERNAL,
    PARTNER
}

enum ParticipationStatus {
    ACTIVE,
    INACTIVE
}

enum ProjectStatus {
    DRAFT,
    ACTIVE,
    PAUSED,
    COMPLETED,
    CANCELED
}

enum ProductStatus {
    ACTIVE,
    INACTIVE
}

enum ItemStatus {
    ACTIVE,
    INACTIVE
}

enum DeliverableType {
    DOCUMENT,
    FORM
}

enum DeliverableStatus {
    PENDING,
    IN_PROGRESS,
    SUBMITTED,
    APPROVED,
    REJECTED,
    CANCELED
}

enum DeliverableDocumentStatus {
    PENDING_UPLOAD,
    AVAILABLE,
    DELETED
}

enum MilestoneTemplateStatus {
    ACTIVE,
    INACTIVE
}

enum ProjectMilestoneStatus {
    PLANNED,
    COMPLETED,
    CANCELED
}

enum OpenIssueStatus {
    OPEN,
    IN_PROGRESS,
    RESOLVED,
    CLOSED,
    CANCELED
}

enum OpenIssueSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

enum DocumentStorageProvider {
    STUB,
    S3
}
