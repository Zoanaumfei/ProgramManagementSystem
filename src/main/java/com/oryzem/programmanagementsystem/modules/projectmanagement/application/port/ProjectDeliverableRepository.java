package com.oryzem.programmanagementsystem.modules.projectmanagement.application.port;

import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectDeliverableAggregate;
import java.util.List;
import java.util.Optional;

public interface ProjectDeliverableRepository {

    Optional<ProjectDeliverableAggregate> findById(String id);

    Optional<ProjectDeliverableAggregate> findByIdAndProjectId(String id, String projectId);

    List<ProjectDeliverableAggregate> findAllByProjectIdOrderByPlannedDueDateAscIdAsc(String projectId);

    List<ProjectDeliverableAggregate> findAllByProjectIdAndStructureNodeIdOrderByPlannedDueDateAscIdAsc(String projectId, String structureNodeId);

    ProjectDeliverableAggregate save(ProjectDeliverableAggregate deliverable);
}
