package com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataProjectMilestoneJpaRepository extends JpaRepository<ProjectMilestoneEntity, String> {
    List<ProjectMilestoneEntity> findAllByProjectIdOrderBySequenceNoAsc(String projectId);
    List<ProjectMilestoneEntity> findAllByProjectIdAndStructureNodeIdOrderBySequenceNoAsc(String projectId, String structureNodeId);
    Optional<ProjectMilestoneEntity> findByIdAndProjectId(String id, String projectId);
    void deleteAllByProjectId(String projectId);
}
