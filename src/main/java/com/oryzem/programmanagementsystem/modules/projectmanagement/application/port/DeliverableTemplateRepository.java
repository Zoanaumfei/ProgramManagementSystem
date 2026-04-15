package com.oryzem.programmanagementsystem.modules.projectmanagement.application.port;

import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.DeliverableTemplateAggregate;
import java.util.List;
import java.util.Optional;

public interface DeliverableTemplateRepository {

    Optional<DeliverableTemplateAggregate> findById(String id);

    List<DeliverableTemplateAggregate> findAllByTemplateIdOrderByPlannedDueOffsetDaysAscCodeAsc(String templateId);

    List<DeliverableTemplateAggregate> findAllByTemplateIdInOrderByTemplateIdAscPlannedDueOffsetDaysAscCodeAsc(List<String> templateIds);

    DeliverableTemplateAggregate save(DeliverableTemplateAggregate deliverableTemplate);
}
