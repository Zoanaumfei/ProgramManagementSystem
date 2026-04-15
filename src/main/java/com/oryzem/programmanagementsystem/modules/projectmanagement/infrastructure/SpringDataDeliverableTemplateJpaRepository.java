package com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataDeliverableTemplateJpaRepository extends JpaRepository<DeliverableTemplateEntity, String> {
    List<DeliverableTemplateEntity> findAllByTemplateIdOrderByPlannedDueOffsetDaysAscCodeAsc(String templateId);
    List<DeliverableTemplateEntity> findAllByTemplateIdInOrderByTemplateIdAscPlannedDueOffsetDaysAscCodeAsc(List<String> templateIds);
}
