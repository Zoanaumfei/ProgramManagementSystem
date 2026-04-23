package com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure;

import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectTemplateStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataProjectTemplateJpaRepository extends JpaRepository<ProjectTemplateEntity, String> {
    Optional<ProjectTemplateEntity> findByFrameworkTypeAndIsDefaultTrueAndStatus(String frameworkType, ProjectTemplateStatus status);
    List<ProjectTemplateEntity> findAllByStructureTemplateIdOrderByFrameworkTypeAscVersionDesc(String structureTemplateId);
    List<ProjectTemplateEntity> findAllByOrderByFrameworkTypeAscVersionDesc();
}
