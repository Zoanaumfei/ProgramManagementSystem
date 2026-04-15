package com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataDeliverableSubmissionDocumentJpaRepository extends JpaRepository<DeliverableSubmissionDocumentEntity, String> {
    List<DeliverableSubmissionDocumentEntity> findAllBySubmissionId(String submissionId);
}
