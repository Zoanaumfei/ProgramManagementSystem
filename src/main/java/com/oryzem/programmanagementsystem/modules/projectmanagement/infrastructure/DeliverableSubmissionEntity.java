package com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure;

import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.DeliverableSubmissionAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.DeliverableSubmissionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

@Entity
@Table(name = "deliverable_submission")
public class DeliverableSubmissionEntity {
    @Id
    @Column(length = 64, nullable = false)
    private String id;
    @Column(name = "deliverable_id", length = 64, nullable = false)
    private String deliverableId;
    @Column(name = "submission_number", nullable = false)
    private int submissionNumber;
    @Column(name = "submitted_by_user_id", length = 64, nullable = false)
    private String submittedByUserId;
    @Column(name = "submitted_by_organization_id", length = 64, nullable = false)
    private String submittedByOrganizationId;
    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;
    @Enumerated(EnumType.STRING)
    @Column(length = 32, nullable = false)
    private DeliverableSubmissionStatus status;
    @Column(name = "review_comment", columnDefinition = "TEXT")
    private String reviewComment;
    @Column(name = "reviewed_by_user_id", length = 64)
    private String reviewedByUserId;
    @Column(name = "reviewed_at")
    private Instant reviewedAt;
    @Version
    @Column(nullable = false)
    private long version;

    protected DeliverableSubmissionEntity() {}

    public static DeliverableSubmissionEntity create(DeliverableSubmissionAggregate aggregate) {
        DeliverableSubmissionEntity entity = new DeliverableSubmissionEntity();
        entity.id = aggregate.id();
        entity.deliverableId = aggregate.deliverableId();
        entity.submissionNumber = aggregate.submissionNumber();
        entity.submittedByUserId = aggregate.submittedByUserId();
        entity.submittedByOrganizationId = aggregate.submittedByOrganizationId();
        entity.submittedAt = aggregate.submittedAt();
        entity.status = aggregate.status();
        entity.reviewComment = aggregate.reviewComment();
        entity.reviewedByUserId = aggregate.reviewedByUserId();
        entity.reviewedAt = aggregate.reviewedAt();
        entity.version = aggregate.version();
        return entity;
    }

    public DeliverableSubmissionAggregate toDomain() {
        return new DeliverableSubmissionAggregate(id, deliverableId, submissionNumber, submittedByUserId, submittedByOrganizationId, submittedAt, status, reviewComment, reviewedByUserId, reviewedAt, version);
    }

    public void apply(DeliverableSubmissionAggregate aggregate) {
        this.status = aggregate.status();
        this.reviewComment = aggregate.reviewComment();
        this.reviewedByUserId = aggregate.reviewedByUserId();
        this.reviewedAt = aggregate.reviewedAt();
    }

    public String getId() { return id; }
    public String getDeliverableId() { return deliverableId; }
    public int getSubmissionNumber() { return submissionNumber; }
    public String getSubmittedByUserId() { return submittedByUserId; }
    public String getSubmittedByOrganizationId() { return submittedByOrganizationId; }
    public Instant getSubmittedAt() { return submittedAt; }
    public DeliverableSubmissionStatus getStatus() { return status; }
    public String getReviewComment() { return reviewComment; }
    public String getReviewedByUserId() { return reviewedByUserId; }
    public Instant getReviewedAt() { return reviewedAt; }
    public long getVersion() { return version; }
}
