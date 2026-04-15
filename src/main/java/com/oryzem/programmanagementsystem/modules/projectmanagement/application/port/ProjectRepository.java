package com.oryzem.programmanagementsystem.modules.projectmanagement.application.port;

import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectAggregate;
import java.util.List;
import java.util.Optional;

public interface ProjectRepository {

    Optional<ProjectAggregate> findById(String id);

    Optional<ProjectAggregate> findByIdAndTenantId(String id, String tenantId);

    boolean existsByTenantIdAndCodeIgnoreCase(String tenantId, String code);

    List<ProjectAggregate> findAllByTenantIdOrderByCreatedAtDescIdDesc(String tenantId);

    ProjectAggregate save(ProjectAggregate project);
}
