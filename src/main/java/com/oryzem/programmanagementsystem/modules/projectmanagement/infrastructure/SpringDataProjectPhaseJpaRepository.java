package com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataProjectPhaseJpaRepository extends JpaRepository<ProjectPhaseEntity, String> {
    List<ProjectPhaseEntity> findAllByProjectIdOrderBySequenceNoAsc(String projectId);
}
