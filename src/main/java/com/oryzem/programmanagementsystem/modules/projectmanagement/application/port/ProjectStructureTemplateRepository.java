package com.oryzem.programmanagementsystem.modules.projectmanagement.application.port;

import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectStructureTemplateAggregate;
import java.util.List;
import java.util.Optional;

public interface ProjectStructureTemplateRepository {

    Optional<ProjectStructureTemplateAggregate> findById(String id);

    List<ProjectStructureTemplateAggregate> findAllByOrderByFrameworkTypeAscVersionDescNameAsc();

    List<ProjectStructureTemplateAggregate> findAllByActiveTrueOrderByFrameworkTypeAscVersionDescNameAsc();

    ProjectStructureTemplateAggregate save(ProjectStructureTemplateAggregate structureTemplate);

    void deleteById(String id);
}
