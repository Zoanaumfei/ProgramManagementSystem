package com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataProjectMilestoneTemplateJpaRepository extends JpaRepository<ProjectMilestoneTemplateEntity, String> {
    List<ProjectMilestoneTemplateEntity> findAllByTemplateIdOrderBySequenceNoAsc(String templateId);
    List<ProjectMilestoneTemplateEntity> findAllByTemplateIdInOrderByTemplateIdAscSequenceNoAsc(List<String> templateIds);
}
