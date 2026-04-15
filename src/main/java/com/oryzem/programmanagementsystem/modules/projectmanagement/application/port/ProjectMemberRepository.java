package com.oryzem.programmanagementsystem.modules.projectmanagement.application.port;

import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectMemberAggregate;
import java.util.List;
import java.util.Optional;

public interface ProjectMemberRepository {

    List<ProjectMemberAggregate> findAllByProjectIdAndActiveTrueOrderByAssignedAtAsc(String projectId);

    Optional<ProjectMemberAggregate> findByProjectIdAndUserIdAndActiveTrue(String projectId, String userId);

    List<ProjectMemberAggregate> findAllByProjectIdAndUserId(String projectId, String userId);

    List<ProjectMemberAggregate> findAllByUserIdAndActiveTrue(String userId);

    ProjectMemberAggregate save(ProjectMemberAggregate member);
}
