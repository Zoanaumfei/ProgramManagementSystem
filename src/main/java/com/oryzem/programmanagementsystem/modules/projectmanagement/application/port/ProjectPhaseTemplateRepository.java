package com.oryzem.programmanagementsystem.modules.projectmanagement.application.port;

import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectPhaseTemplateAggregate;
import java.util.List;
import java.util.Optional;

public interface ProjectPhaseTemplateRepository {

    Optional<ProjectPhaseTemplateAggregate> findById(String id);

    List<ProjectPhaseTemplateAggregate> findAllByTemplateIdOrderBySequenceNoAsc(String templateId);

    ProjectPhaseTemplateAggregate save(ProjectPhaseTemplateAggregate phaseTemplate);
}
