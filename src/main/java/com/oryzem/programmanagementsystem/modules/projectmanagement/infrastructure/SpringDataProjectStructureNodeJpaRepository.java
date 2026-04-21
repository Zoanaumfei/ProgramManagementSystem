package com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataProjectStructureNodeJpaRepository extends JpaRepository<ProjectStructureNodeEntity, String> {
    List<ProjectStructureNodeEntity> findAllByProjectIdOrderBySequenceNoAscIdAsc(String projectId);
    List<ProjectStructureNodeEntity> findAllByProjectIdAndParentNodeIdOrderBySequenceNoAscIdAsc(String projectId, String parentNodeId);
    Optional<ProjectStructureNodeEntity> findByIdAndProjectId(String id, String projectId);
    void deleteAllByProjectId(String projectId);
}
