package com.oryzem.programmanagementsystem.modules.projectmanagement.application.port;

import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectMilestoneTemplateAggregate;
import java.util.List;
import java.util.Optional;

public interface ProjectMilestoneTemplateRepository {

    Optional<ProjectMilestoneTemplateAggregate> findById(String id);

    List<ProjectMilestoneTemplateAggregate> findAllByTemplateIdOrderBySequenceNoAsc(String templateId);

    List<ProjectMilestoneTemplateAggregate> findAllByTemplateIdInOrderByTemplateIdAscSequenceNoAsc(List<String> templateIds);

    ProjectMilestoneTemplateAggregate save(ProjectMilestoneTemplateAggregate milestoneTemplate);
}
