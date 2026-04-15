package com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure.mapper;

import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.DeliverableSubmissionDocumentAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure.DeliverableSubmissionDocumentEntity;
import org.springframework.stereotype.Component;

@Component
public class DeliverableSubmissionDocumentPersistenceMapper {

    public DeliverableSubmissionDocumentAggregate toAggregate(DeliverableSubmissionDocumentEntity entity) {
        return new DeliverableSubmissionDocumentAggregate(
                entity.getId(),
                entity.getSubmissionId(),
                entity.getDocumentId());
    }

    public DeliverableSubmissionDocumentEntity toEntity(DeliverableSubmissionDocumentAggregate aggregate) {
        return DeliverableSubmissionDocumentEntity.create(
                aggregate.id(),
                aggregate.submissionId(),
                aggregate.documentId());
    }
}
