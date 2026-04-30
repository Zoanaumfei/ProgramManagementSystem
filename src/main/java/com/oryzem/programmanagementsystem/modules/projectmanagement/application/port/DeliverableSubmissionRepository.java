package com.oryzem.programmanagementsystem.modules.projectmanagement.application.port;

import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.DeliverableSubmissionStatus;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.DeliverableSubmissionAggregate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DeliverableSubmissionRepository {

    Optional<DeliverableSubmissionAggregate> findById(String id);

    Optional<DeliverableSubmissionAggregate> findByIdAndDeliverableId(String id, String deliverableId);

    Optional<DeliverableSubmissionAggregate> findTopByDeliverableIdOrderBySubmissionNumberDesc(String deliverableId);

    boolean existsByDeliverableIdAndStatusIn(String deliverableId, Collection<DeliverableSubmissionStatus> statuses);

    boolean existsByDeliverableId(String deliverableId);

    List<DeliverableSubmissionAggregate> findAllByDeliverableIdOrderBySubmissionNumberDesc(String deliverableId);

    DeliverableSubmissionAggregate save(DeliverableSubmissionAggregate submission);
}
