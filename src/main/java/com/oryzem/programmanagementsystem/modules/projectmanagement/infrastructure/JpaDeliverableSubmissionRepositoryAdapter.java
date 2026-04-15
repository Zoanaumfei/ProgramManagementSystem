package com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.DeliverableSubmissionRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.DeliverableSubmissionAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.DeliverableSubmissionStatus;
import com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure.mapper.DeliverableSubmissionPersistenceMapper;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaDeliverableSubmissionRepositoryAdapter implements DeliverableSubmissionRepository {

    private final SpringDataDeliverableSubmissionJpaRepository delegate;
    private final DeliverableSubmissionPersistenceMapper mapper;

    public JpaDeliverableSubmissionRepositoryAdapter(
            SpringDataDeliverableSubmissionJpaRepository delegate,
            DeliverableSubmissionPersistenceMapper mapper) {
        this.delegate = delegate;
        this.mapper = mapper;
    }

    @Override
    public Optional<DeliverableSubmissionAggregate> findById(String id) {
        return delegate.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<DeliverableSubmissionAggregate> findByIdAndDeliverableId(String id, String deliverableId) {
        return delegate.findByIdAndDeliverableId(id, deliverableId).map(mapper::toDomain);
    }

    @Override
    public Optional<DeliverableSubmissionAggregate> findTopByDeliverableIdOrderBySubmissionNumberDesc(String deliverableId) {
        return delegate.findTopByDeliverableIdOrderBySubmissionNumberDesc(deliverableId).map(mapper::toDomain);
    }

    @Override
    public boolean existsByDeliverableIdAndStatusIn(String deliverableId, Collection<DeliverableSubmissionStatus> statuses) {
        return delegate.existsByDeliverableIdAndStatusIn(deliverableId, statuses);
    }

    @Override
    public List<DeliverableSubmissionAggregate> findAllByDeliverableIdOrderBySubmissionNumberDesc(String deliverableId) {
        return delegate.findAllByDeliverableIdOrderBySubmissionNumberDesc(deliverableId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public DeliverableSubmissionAggregate save(DeliverableSubmissionAggregate submission) {
        DeliverableSubmissionEntity entity = delegate.findById(submission.id())
                .map(existing -> {
                    mapper.apply(existing, submission);
                    return existing;
                })
                .orElseGet(() -> mapper.toNewEntity(submission));
        return mapper.toDomain(delegate.save(entity));
    }
}
