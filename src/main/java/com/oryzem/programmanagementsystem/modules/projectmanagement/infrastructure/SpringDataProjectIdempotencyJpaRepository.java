package com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataProjectIdempotencyJpaRepository extends JpaRepository<ProjectIdempotencyEntity, ProjectIdempotencyEntity.ProjectIdempotencyId> {
}
