package com.oryzem.programmanagementsystem.modules.projectmanagement.application.port;

import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectMilestoneAggregate;
import java.util.List;
import java.util.Optional;

public interface ProjectMilestoneRepository {

    Optional<ProjectMilestoneAggregate> findById(String id);

    Optional<ProjectMilestoneAggregate> findByIdAndProjectId(String id, String projectId);

    List<ProjectMilestoneAggregate> findAllByProjectIdOrderBySequenceNoAsc(String projectId);

    List<ProjectMilestoneAggregate> findAllByProjectIdAndStructureNodeIdOrderBySequenceNoAsc(String projectId, String structureNodeId);

    ProjectMilestoneAggregate save(ProjectMilestoneAggregate milestone);

    void deleteById(String id);
}
