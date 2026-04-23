package com.oryzem.programmanagementsystem.modules.projectmanagement.application.port;

import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectTemplateAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectTemplateStatus;
import java.util.List;
import java.util.Optional;

public interface ProjectTemplateRepository {

    Optional<ProjectTemplateAggregate> findById(String id);

    Optional<ProjectTemplateAggregate> findByFrameworkTypeAndIsDefaultTrueAndStatus(String frameworkType, ProjectTemplateStatus status);

    List<ProjectTemplateAggregate> findAllByStructureTemplateIdOrderByFrameworkTypeAscVersionDesc(String structureTemplateId);

    List<ProjectTemplateAggregate> findAllByOrderByFrameworkTypeAscVersionDesc();

    ProjectTemplateAggregate save(ProjectTemplateAggregate projectTemplate);

    List<ProjectTemplateAggregate> saveAll(Iterable<ProjectTemplateAggregate> projectTemplates);

    void deleteById(String id);
}
