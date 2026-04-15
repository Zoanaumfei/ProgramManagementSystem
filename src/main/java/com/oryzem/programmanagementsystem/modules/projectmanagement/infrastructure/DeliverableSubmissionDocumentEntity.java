package com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "deliverable_submission_document")
public class DeliverableSubmissionDocumentEntity {
    @Id
    @Column(length = 64, nullable = false)
    private String id;
    @Column(name = "submission_id", length = 64, nullable = false)
    private String submissionId;
    @Column(name = "document_id", length = 64, nullable = false)
    private String documentId;

    protected DeliverableSubmissionDocumentEntity() {}

    public static DeliverableSubmissionDocumentEntity create(String id, String submissionId, String documentId) {
        DeliverableSubmissionDocumentEntity entity = new DeliverableSubmissionDocumentEntity();
        entity.id = id;
        entity.submissionId = submissionId;
        entity.documentId = documentId;
        return entity;
    }

    public String getId() { return id; }
    public String getSubmissionId() { return submissionId; }
    public String getDocumentId() { return documentId; }
}
