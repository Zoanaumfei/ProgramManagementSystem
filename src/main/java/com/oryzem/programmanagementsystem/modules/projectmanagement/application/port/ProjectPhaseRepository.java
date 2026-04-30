package com.oryzem.programmanagementsystem.modules.projectmanagement.application.port;

import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectPhaseAggregate;
import java.util.List;
import java.util.Optional;

public interface ProjectPhaseRepository {

    List<ProjectPhaseAggregate> findAllByProjectIdOrderBySequenceNoAsc(String projectId);

    Optional<ProjectPhaseAggregate> findByIdAndProjectId(String id, String projectId);

    ProjectPhaseAggregate save(ProjectPhaseAggregate phase);
}
