package com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.DeliverableTemplateRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.DeliverableTemplateAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure.mapper.DeliverableTemplatePersistenceMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaDeliverableTemplateRepositoryAdapter implements DeliverableTemplateRepository {

    private final SpringDataDeliverableTemplateJpaRepository delegate;
    private final DeliverableTemplatePersistenceMapper mapper;

    public JpaDeliverableTemplateRepositoryAdapter(
            SpringDataDeliverableTemplateJpaRepository delegate,
            DeliverableTemplatePersistenceMapper mapper) {
        this.delegate = delegate;
        this.mapper = mapper;
    }

    @Override
    public Optional<DeliverableTemplateAggregate> findById(String id) {
        return delegate.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<DeliverableTemplateAggregate> findAllByTemplateIdOrderByPlannedDueOffsetDaysAscCodeAsc(String templateId) {
        return delegate.findAllByTemplateIdOrderByPlannedDueOffsetDaysAscCodeAsc(templateId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<DeliverableTemplateAggregate> findAllByTemplateIdInOrderByTemplateIdAscPlannedDueOffsetDaysAscCodeAsc(List<String> templateIds) {
        return delegate.findAllByTemplateIdInOrderByTemplateIdAscPlannedDueOffsetDaysAscCodeAsc(templateIds).stream().map(mapper::toDomain).toList();
    }

    @Override
    public DeliverableTemplateAggregate save(DeliverableTemplateAggregate deliverableTemplate) {
        if (delegate.existsById(deliverableTemplate.id())) {
            DeliverableTemplateEntity entity = delegate.findById(deliverableTemplate.id()).orElseThrow();
            mapper.apply(entity, deliverableTemplate);
            return mapper.toDomain(delegate.save(entity));
        }
        return mapper.toDomain(delegate.save(mapper.toNewEntity(deliverableTemplate)));
    }
}
