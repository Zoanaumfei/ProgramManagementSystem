package com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataProjectStructureLevelTemplateJpaRepository extends JpaRepository<ProjectStructureLevelTemplateEntity, String> {
    List<ProjectStructureLevelTemplateEntity> findAllByStructureTemplateIdOrderBySequenceNoAsc(String structureTemplateId);
}
