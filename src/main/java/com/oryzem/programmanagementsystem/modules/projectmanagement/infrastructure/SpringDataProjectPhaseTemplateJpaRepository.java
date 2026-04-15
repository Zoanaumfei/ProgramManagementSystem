package com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataProjectPhaseTemplateJpaRepository extends JpaRepository<ProjectPhaseTemplateEntity, String> {
    List<ProjectPhaseTemplateEntity> findAllByTemplateIdOrderBySequenceNoAsc(String templateId);
}
