package com.oryzem.programmanagementsystem.modules.projectmanagement.application.model.read;

import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.DeliverableSubmissionStatus;
import java.time.Instant;
import java.util.List;

public final class SubmissionReadModels {

    private SubmissionReadModels() {
    }

    public record DeliverableSubmissionReadModel(
            String id,
            int submissionNumber,
            String submittedByUserId,
            String submittedByOrganizationId,
            Instant submittedAt,
            DeliverableSubmissionStatus status,
            String reviewComment,
            String reviewedByUserId,
            Instant reviewedAt,
            long version,
            List<String> documentIds) {
    }
}
