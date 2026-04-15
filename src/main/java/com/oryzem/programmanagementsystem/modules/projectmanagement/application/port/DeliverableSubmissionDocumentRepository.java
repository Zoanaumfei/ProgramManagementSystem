package com.oryzem.programmanagementsystem.modules.projectmanagement.application.port;

import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.DeliverableSubmissionDocumentAggregate;
import java.util.List;

public interface DeliverableSubmissionDocumentRepository {

    DeliverableSubmissionDocumentAggregate save(DeliverableSubmissionDocumentAggregate document);

    List<DeliverableSubmissionDocumentAggregate> findAllBySubmissionId(String submissionId);
}
