package com.oryzem.programmanagementsystem.modules.projectmanagement.application.port;

import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectStructureNodeAggregate;
import java.util.List;
import java.util.Optional;

public interface ProjectStructureNodeRepository {

    Optional<ProjectStructureNodeAggregate> findById(String id);

    Optional<ProjectStructureNodeAggregate> findByIdAndProjectId(String id, String projectId);

    List<ProjectStructureNodeAggregate> findAllByProjectIdOrderBySequenceNoAscIdAsc(String projectId);

    List<ProjectStructureNodeAggregate> findAllByProjectIdAndParentNodeIdOrderBySequenceNoAscIdAsc(String projectId, String parentNodeId);

    ProjectStructureNodeAggregate save(ProjectStructureNodeAggregate node);
}
