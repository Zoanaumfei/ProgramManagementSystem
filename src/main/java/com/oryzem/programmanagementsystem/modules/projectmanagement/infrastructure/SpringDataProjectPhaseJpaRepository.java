package com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataProjectPhaseJpaRepository extends JpaRepository<ProjectPhaseEntity, String> {
    List<ProjectPhaseEntity> findAllByProjectIdOrderBySequenceNoAsc(String projectId);
    Optional<ProjectPhaseEntity> findByIdAndProjectId(String id, String projectId);
    void deleteAllByProjectId(String projectId);
}
