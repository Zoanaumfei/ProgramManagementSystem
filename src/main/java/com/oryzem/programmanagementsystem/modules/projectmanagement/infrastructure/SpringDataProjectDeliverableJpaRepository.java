package com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataProjectDeliverableJpaRepository extends JpaRepository<ProjectDeliverableEntity, String> {
    List<ProjectDeliverableEntity> findAllByProjectIdOrderByPlannedDueDateAscIdAsc(String projectId);
    List<ProjectDeliverableEntity> findAllByProjectIdAndStructureNodeIdOrderByPlannedDueDateAscIdAsc(String projectId, String structureNodeId);
    Optional<ProjectDeliverableEntity> findByIdAndProjectId(String id, String projectId);
    void deleteAllByProjectId(String projectId);
}
