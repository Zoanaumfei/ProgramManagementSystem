package com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataDeliverableSubmissionJpaRepository extends JpaRepository<DeliverableSubmissionEntity, String> {
    List<DeliverableSubmissionEntity> findAllByDeliverableIdOrderBySubmissionNumberDesc(String deliverableId);
    Optional<DeliverableSubmissionEntity> findByIdAndDeliverableId(String id, String deliverableId);
    Optional<DeliverableSubmissionEntity> findTopByDeliverableIdOrderBySubmissionNumberDesc(String deliverableId);
    boolean existsByDeliverableIdAndStatusIn(String deliverableId, java.util.Collection<com.oryzem.programmanagementsystem.modules.projectmanagement.domain.DeliverableSubmissionStatus> statuses);
    void deleteAllByDeliverableIdIn(java.util.Collection<String> deliverableIds);
}
