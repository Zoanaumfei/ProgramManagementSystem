package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.DeliverableSubmissionStatus;
import java.time.Instant;
import java.util.List;

public final class SubmissionViews {

    private SubmissionViews() {
    }

    public record DeliverableSubmissionView(
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
