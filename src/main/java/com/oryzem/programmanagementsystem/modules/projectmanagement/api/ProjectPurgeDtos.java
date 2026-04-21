package com.oryzem.programmanagementsystem.modules.projectmanagement.api;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.ProjectPurgeViews;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

public final class ProjectPurgeDtos {

    private ProjectPurgeDtos() {
    }

    public record CreateProjectPurgeIntentRequest(
            @NotBlank String reason) {
    }

    public record ExecuteProjectPurgeRequest(
            @NotBlank String reason,
            @NotBlank String purgeToken,
            boolean confirm,
            @NotBlank String confirmationText) {
    }

    public record ProjectPurgeImpactResponse(
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

        static ProjectPurgeImpactResponse from(ProjectPurgeViews.ProjectPurgeImpactView view) {
            return new ProjectPurgeImpactResponse(
                    view.organizationCount(),
                    view.memberCount(),
                    view.phaseCount(),
                    view.milestoneCount(),
                    view.deliverableCount(),
                    view.submissionCount(),
                    view.submissionDocumentLinkCount(),
                    view.structureNodeCount(),
                    view.documentCount(),
                    view.storageObjectCount());
        }
    }

    public record ProjectPurgeIntentResponse(
            String projectId,
            String projectCode,
            String projectName,
            String reason,
            String purgeToken,
            Instant expiresAt,
            boolean requiresFinalConfirmation,
            ProjectPurgeImpactResponse impact) {

        static ProjectPurgeIntentResponse from(ProjectPurgeViews.ProjectPurgeIntentView view) {
            return new ProjectPurgeIntentResponse(
                    view.projectId(),
                    view.projectCode(),
                    view.projectName(),
                    view.reason(),
                    view.purgeToken(),
                    view.expiresAt(),
                    view.requiresFinalConfirmation(),
                    ProjectPurgeImpactResponse.from(view.impact()));
        }
    }

    public record ProjectPurgeExecutionResponse(
            String projectId,
            String projectCode,
            String projectName,
            String reason,
            String status,
            Instant purgedAt,
            ProjectPurgeImpactResponse impact) {

        static ProjectPurgeExecutionResponse from(ProjectPurgeViews.ProjectPurgeResultView view) {
            return new ProjectPurgeExecutionResponse(
                    view.projectId(),
                    view.projectCode(),
                    view.projectName(),
                    view.reason(),
                    view.status(),
                    view.purgedAt(),
                    ProjectPurgeImpactResponse.from(view.impact()));
        }
    }
}
