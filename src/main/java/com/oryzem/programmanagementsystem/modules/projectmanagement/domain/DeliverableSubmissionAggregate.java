package com.oryzem.programmanagementsystem.modules.projectmanagement.domain;

import com.oryzem.programmanagementsystem.platform.shared.BusinessRuleException;
import java.time.Instant;
import java.util.Map;

public record DeliverableSubmissionAggregate(
        String id,
        String deliverableId,
        int submissionNumber,
        String submittedByUserId,
        String submittedByOrganizationId,
        Instant submittedAt,
        DeliverableSubmissionStatus status,
        String reviewComment,
        String reviewedByUserId,
        Instant reviewedAt,
        long version) {

    public DeliverableSubmissionAggregate startReview() {
        if (status != DeliverableSubmissionStatus.SUBMITTED && status != DeliverableSubmissionStatus.UNDER_REVIEW) {
            throw new BusinessRuleException(
                    "SUBMISSION_REVIEW_NOT_ALLOWED",
                    "Only submitted submissions can enter review.",
                    Map.of("status", status.name()));
        }
        return new DeliverableSubmissionAggregate(
                id,
                deliverableId,
                submissionNumber,
                submittedByUserId,
                submittedByOrganizationId,
                submittedAt,
                DeliverableSubmissionStatus.UNDER_REVIEW,
                reviewComment,
                reviewedByUserId,
                reviewedAt,
                version);
    }

    public DeliverableSubmissionAggregate approve(String reviewerUserId, String comment, Instant when) {
        if (status != DeliverableSubmissionStatus.SUBMITTED && status != DeliverableSubmissionStatus.UNDER_REVIEW) {
            throw new BusinessRuleException(
                    "SUBMISSION_APPROVAL_NOT_ALLOWED",
                    "Only submitted submissions can be approved.",
                    Map.of("status", status.name()));
        }
        return new DeliverableSubmissionAggregate(
                id,
                deliverableId,
                submissionNumber,
                submittedByUserId,
                submittedByOrganizationId,
                submittedAt,
                DeliverableSubmissionStatus.APPROVED,
                comment,
                reviewerUserId,
                when,
                version);
    }

    public DeliverableSubmissionAggregate reject(String reviewerUserId, String comment, Instant when) {
        if (status != DeliverableSubmissionStatus.SUBMITTED && status != DeliverableSubmissionStatus.UNDER_REVIEW) {
            throw new BusinessRuleException(
                    "SUBMISSION_REJECTION_NOT_ALLOWED",
                    "Only submitted submissions can be rejected.",
                    Map.of("status", status.name()));
        }
        return new DeliverableSubmissionAggregate(
                id,
                deliverableId,
                submissionNumber,
                submittedByUserId,
                submittedByOrganizationId,
                submittedAt,
                DeliverableSubmissionStatus.REJECTED,
                comment,
                reviewerUserId,
                when,
                version);
    }
}
