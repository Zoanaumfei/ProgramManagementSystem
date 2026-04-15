package com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure.mapper;

import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.DeliverableSubmissionAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure.DeliverableSubmissionEntity;
import org.springframework.stereotype.Component;

@Component
public class DeliverableSubmissionPersistenceMapper {

    public DeliverableSubmissionAggregate toDomain(DeliverableSubmissionEntity entity) {
        return entity.toDomain();
    }

    public DeliverableSubmissionEntity toNewEntity(DeliverableSubmissionAggregate aggregate) {
        return DeliverableSubmissionEntity.create(aggregate);
    }

    public void apply(DeliverableSubmissionEntity entity, DeliverableSubmissionAggregate aggregate) {
        entity.apply(aggregate);
    }
}
