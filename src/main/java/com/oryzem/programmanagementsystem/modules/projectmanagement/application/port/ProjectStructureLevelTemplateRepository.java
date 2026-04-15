package com.oryzem.programmanagementsystem.modules.projectmanagement.application.port;

import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectStructureLevelTemplateAggregate;
import java.util.List;
import java.util.Optional;

public interface ProjectStructureLevelTemplateRepository {

    Optional<ProjectStructureLevelTemplateAggregate> findById(String id);

    List<ProjectStructureLevelTemplateAggregate> findAllByStructureTemplateIdOrderBySequenceNoAsc(String structureTemplateId);

    ProjectStructureLevelTemplateAggregate save(ProjectStructureLevelTemplateAggregate levelTemplate);

    List<ProjectStructureLevelTemplateAggregate> saveAll(Iterable<ProjectStructureLevelTemplateAggregate> levelTemplates);

    List<ProjectStructureLevelTemplateAggregate> saveAllAndFlush(Iterable<ProjectStructureLevelTemplateAggregate> levelTemplates);
}
