package com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.DeliverableSubmissionDocumentRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.DeliverableSubmissionDocumentAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure.mapper.DeliverableSubmissionDocumentPersistenceMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class JpaDeliverableSubmissionDocumentRepositoryAdapter implements DeliverableSubmissionDocumentRepository {

    private final SpringDataDeliverableSubmissionDocumentJpaRepository delegate;
    private final DeliverableSubmissionDocumentPersistenceMapper mapper;

    public JpaDeliverableSubmissionDocumentRepositoryAdapter(
            SpringDataDeliverableSubmissionDocumentJpaRepository delegate,
            DeliverableSubmissionDocumentPersistenceMapper mapper) {
        this.delegate = delegate;
        this.mapper = mapper;
    }

    @Override
    public DeliverableSubmissionDocumentAggregate save(DeliverableSubmissionDocumentAggregate document) {
        return mapper.toAggregate(delegate.save(mapper.toEntity(document)));
    }

    @Override
    public List<DeliverableSubmissionDocumentAggregate> findAllBySubmissionId(String submissionId) {
        return delegate.findAllBySubmissionId(submissionId).stream()
                .map(mapper::toAggregate)
                .toList();
    }
}
