package com.oryzem.programmanagementsystem.modules.projectmanagement.application.port;

import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectOrganizationAggregate;
import java.util.List;
import java.util.Optional;

public interface ProjectOrganizationRepository {

    List<ProjectOrganizationAggregate> findAllByProjectIdAndActiveTrueOrderByJoinedAtAsc(String projectId);

    Optional<ProjectOrganizationAggregate> findByProjectIdAndOrganizationIdAndActiveTrue(String projectId, String organizationId);

    boolean existsByProjectIdAndOrganizationId(String projectId, String organizationId);

    List<ProjectOrganizationAggregate> findAllByOrganizationIdAndActiveTrue(String organizationId);

    ProjectOrganizationAggregate save(ProjectOrganizationAggregate organization);
}
