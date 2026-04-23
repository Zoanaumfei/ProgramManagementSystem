package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectFrameworkRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectFrameworkAggregate;
import com.oryzem.programmanagementsystem.platform.shared.BusinessRuleException;
import com.oryzem.programmanagementsystem.platform.shared.ResourceNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class ProjectFrameworkCatalogService {

    private final ProjectFrameworkRepository projectFrameworkRepository;

    public ProjectFrameworkCatalogService(ProjectFrameworkRepository projectFrameworkRepository) {
        this.projectFrameworkRepository = projectFrameworkRepository;
    }

    public ProjectFrameworkAggregate requireFramework(String frameworkCode) {
        return projectFrameworkRepository.findByCode(frameworkCode)
                .orElseThrow(() -> new ResourceNotFoundException("ProjectFramework", frameworkCode));
    }

    public ProjectFrameworkAggregate requireActiveFramework(String frameworkCode) {
        ProjectFrameworkAggregate framework = requireFramework(frameworkCode);
        if (!framework.active()) {
            throw new BusinessRuleException("PROJECT_FRAMEWORK_INACTIVE", "Inactive project frameworks cannot be used for new templates or projects.");
        }
        return framework;
    }
}
