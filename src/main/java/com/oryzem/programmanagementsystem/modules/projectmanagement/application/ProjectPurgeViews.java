package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import java.time.Instant;

public final class ProjectPurgeViews {

    private ProjectPurgeViews() {
    }

    public record ProjectPurgeImpactView(
            long organizationCount,
            long memberCount,
            long phaseCount,
            long milestoneCount,
            long deliverableCount,
            long submissionCount,
            long submissionDocumentLinkCount,
            long structureNodeCount,
            long documentCount,
            long storageObjectCount) {
    }

    public record ProjectPurgeIntentView(
            String projectId,
            String projectCode,
            String projectName,
            String reason,
            String purgeToken,
            Instant expiresAt,
            boolean requiresFinalConfirmation,
            ProjectPurgeImpactView impact) {
    }

    public record ProjectPurgeResultView(
            String projectId,
            String projectCode,
            String projectName,
            String reason,
            String status,
            Instant purgedAt,
            ProjectPurgeImpactView impact) {
    }
}
