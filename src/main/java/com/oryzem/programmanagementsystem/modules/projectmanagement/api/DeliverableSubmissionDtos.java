package com.oryzem.programmanagementsystem.modules.projectmanagement.api;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.SubmissionViews;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.model.read.SubmissionReadModels;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.DeliverableSubmissionStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.Instant;
import java.util.List;

public final class DeliverableSubmissionDtos {

    private DeliverableSubmissionDtos() {
    }

    public record SubmitDeliverableRequest(
            @NotNull @PositiveOrZero Long deliverableVersion,
            @NotNull List<@NotBlank String> documentIds) {
    }

    public record ReviewSubmissionRequest(
            String reviewComment,
            @NotNull @PositiveOrZero Long version) {
    }

    public record DeliverableSubmissionResponse(String id, int submissionNumber, String submittedByUserId, String submittedByOrganizationId, Instant submittedAt, DeliverableSubmissionStatus status, String reviewComment, String reviewedByUserId, Instant reviewedAt, long version, List<String> documentIds) {
        static DeliverableSubmissionResponse from(SubmissionViews.DeliverableSubmissionView view) {
            return new DeliverableSubmissionResponse(view.id(), view.submissionNumber(), view.submittedByUserId(), view.submittedByOrganizationId(), view.submittedAt(), view.status(), view.reviewComment(), view.reviewedByUserId(), view.reviewedAt(), view.version(), view.documentIds());
        }

        static DeliverableSubmissionResponse from(SubmissionReadModels.DeliverableSubmissionReadModel readModel) {
            return new DeliverableSubmissionResponse(readModel.id(), readModel.submissionNumber(), readModel.submittedByUserId(), readModel.submittedByOrganizationId(), readModel.submittedAt(), readModel.status(), readModel.reviewComment(), readModel.reviewedByUserId(), readModel.reviewedAt(), readModel.version(), readModel.documentIds());
        }
    }
}
