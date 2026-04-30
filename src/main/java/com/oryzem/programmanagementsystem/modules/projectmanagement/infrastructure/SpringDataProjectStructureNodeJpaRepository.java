package com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataProjectStructureNodeJpaRepository extends JpaRepository<ProjectStructureNodeEntity, String> {
    List<ProjectStructureNodeEntity> findAllByProjectIdOrderBySequenceNoAscIdAsc(String projectId);
    List<ProjectStructureNodeEntity> findAllByProjectIdAndParentNodeIdOrderBySequenceNoAscIdAsc(String projectId, String parentNodeId);
    Optional<ProjectStructureNodeEntity> findByIdAndProjectId(String id, String projectId);
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from ProjectStructureNodeEntity node where node.projectId = :projectId")
    void deleteAllByProjectId(@Param("projectId") String projectId);
}
