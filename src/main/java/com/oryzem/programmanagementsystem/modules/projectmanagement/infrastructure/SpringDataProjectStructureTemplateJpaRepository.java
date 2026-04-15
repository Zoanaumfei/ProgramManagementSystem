package com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataProjectStructureTemplateJpaRepository extends JpaRepository<ProjectStructureTemplateEntity, String> {
    List<ProjectStructureTemplateEntity> findAllByOrderByFrameworkTypeAscVersionDescNameAsc();
    List<ProjectStructureTemplateEntity> findAllByActiveTrueOrderByFrameworkTypeAscVersionDescNameAsc();
}
