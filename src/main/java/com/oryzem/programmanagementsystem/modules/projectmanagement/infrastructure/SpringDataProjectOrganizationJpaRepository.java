package com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataProjectOrganizationJpaRepository extends JpaRepository<ProjectOrganizationEntity, String> {
    java.util.List<ProjectOrganizationEntity> findAllByProjectId(String projectId);
    List<ProjectOrganizationEntity> findAllByProjectIdAndActiveTrueOrderByJoinedAtAsc(String projectId);
    Optional<ProjectOrganizationEntity> findByProjectIdAndOrganizationIdAndActiveTrue(String projectId, String organizationId);
    boolean existsByProjectIdAndOrganizationId(String projectId, String organizationId);
    List<ProjectOrganizationEntity> findAllByOrganizationIdAndActiveTrue(String organizationId);
    void deleteAllByProjectId(String projectId);
}
