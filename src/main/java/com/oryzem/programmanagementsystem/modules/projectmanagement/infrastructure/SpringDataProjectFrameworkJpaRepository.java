package com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataProjectFrameworkJpaRepository extends JpaRepository<ProjectFrameworkEntity, String> {

    Optional<ProjectFrameworkEntity> findByCode(String code);

    boolean existsByCodeIgnoreCase(String code);

    List<ProjectFrameworkEntity> findAllByOrderByDisplayNameAscCodeAsc();
}
