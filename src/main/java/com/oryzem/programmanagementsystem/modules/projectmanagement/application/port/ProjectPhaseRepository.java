package com.oryzem.programmanagementsystem.modules.projectmanagement.application.port;

import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectPhaseAggregate;
import java.util.List;

public interface ProjectPhaseRepository {

    List<ProjectPhaseAggregate> findAllByProjectIdOrderBySequenceNoAsc(String projectId);

    ProjectPhaseAggregate save(ProjectPhaseAggregate phase);
}
