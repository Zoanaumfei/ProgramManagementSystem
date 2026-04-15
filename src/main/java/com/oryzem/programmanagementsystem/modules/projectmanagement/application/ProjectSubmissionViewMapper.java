package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.model.read.SubmissionReadModels;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.DeliverableSubmissionAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.DeliverableSubmissionDocumentAggregate;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ProjectSubmissionViewMapper {

    public SubmissionReadModels.DeliverableSubmissionReadModel toSubmissionReadModel(
            DeliverableSubmissionAggregate aggregate,
            List<DeliverableSubmissionDocumentAggregate> documents) {
        return new SubmissionReadModels.DeliverableSubmissionReadModel(
                aggregate.id(),
                aggregate.submissionNumber(),
                aggregate.submittedByUserId(),
                aggregate.submittedByOrganizationId(),
                aggregate.submittedAt(),
                aggregate.status(),
                aggregate.reviewComment(),
                aggregate.reviewedByUserId(),
                aggregate.reviewedAt(),
                aggregate.version(),
                documents.stream().map(DeliverableSubmissionDocumentAggregate::documentId).toList());
    }

    public SubmissionViews.DeliverableSubmissionView toSubmissionView(
            DeliverableSubmissionAggregate aggregate,
            List<DeliverableSubmissionDocumentAggregate> documents) {
        return new SubmissionViews.DeliverableSubmissionView(
                aggregate.id(),
                aggregate.submissionNumber(),
                aggregate.submittedByUserId(),
                aggregate.submittedByOrganizationId(),
                aggregate.submittedAt(),
                aggregate.status(),
                aggregate.reviewComment(),
                aggregate.reviewedByUserId(),
                aggregate.reviewedAt(),
                aggregate.version(),
                documents.stream().map(DeliverableSubmissionDocumentAggregate::documentId).toList());
    }
}
