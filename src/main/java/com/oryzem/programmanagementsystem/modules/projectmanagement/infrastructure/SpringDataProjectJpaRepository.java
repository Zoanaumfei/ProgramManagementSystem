package com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataProjectJpaRepository extends JpaRepository<ProjectEntity, String> {
    Optional<ProjectEntity> findByIdAndTenantId(String id, String tenantId);
    boolean existsByTenantIdAndCodeIgnoreCase(String tenantId, String code);
    boolean existsByTemplateId(String templateId);
    List<ProjectEntity> findAllByOrderByCreatedAtDescIdDesc();
    List<ProjectEntity> findAllByTenantIdOrderByCreatedAtDescIdDesc(String tenantId);
}
