package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectOrganizationAggregate;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class InstantiateProjectFromTemplateUseCase {

    private final ProjectTemplateInstantiationService instantiationService;

    public InstantiateProjectFromTemplateUseCase(ProjectTemplateInstantiationService instantiationService) {
        this.instantiationService = instantiationService;
    }

    public void execute(ProjectAggregate project, List<ProjectOrganizationAggregate> organizations) {
        instantiationService.instantiate(project, organizations);
    }
}
