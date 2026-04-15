package com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataProjectMemberJpaRepository extends JpaRepository<ProjectMemberEntity, String> {
    List<ProjectMemberEntity> findAllByProjectIdAndActiveTrueOrderByAssignedAtAsc(String projectId);
    Optional<ProjectMemberEntity> findByProjectIdAndUserIdAndActiveTrue(String projectId, String userId);
    List<ProjectMemberEntity> findAllByProjectIdAndUserId(String projectId, String userId);
    List<ProjectMemberEntity> findAllByUserIdAndActiveTrue(String userId);
}
