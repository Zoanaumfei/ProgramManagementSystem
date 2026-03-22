package com.oryzem.programmanagementsystem.modules.projectmanagement;

import com.oryzem.programmanagementsystem.platform.authorization.TenantType;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

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


