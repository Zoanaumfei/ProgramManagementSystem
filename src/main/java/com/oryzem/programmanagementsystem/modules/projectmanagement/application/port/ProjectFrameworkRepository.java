package com.oryzem.programmanagementsystem.modules.projectmanagement.application.port;

import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectFrameworkAggregate;
import java.util.List;
import java.util.Optional;

public interface ProjectFrameworkRepository {

    Optional<ProjectFrameworkAggregate> findById(String id);

    Optional<ProjectFrameworkAggregate> findByCode(String code);

    boolean existsByCodeIgnoreCase(String code);

    List<ProjectFrameworkAggregate> findAllByOrderByDisplayNameAscCodeAsc();

    ProjectFrameworkAggregate save(ProjectFrameworkAggregate framework);
}
