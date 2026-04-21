package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.model.read.TemplateReadModels;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectTemplateRepository;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ListProjectTemplatesUseCase {

    private final ProjectAuthorizationService authorizationService;
    private final ProjectStructureTemplateAdministrationService administrationService;
    private final ProjectTemplateRepository projectTemplateRepository;
    private final ProjectViewMapper viewMapper;

    public ListProjectTemplatesUseCase(
            ProjectAuthorizationService authorizationService,
            ProjectStructureTemplateAdministrationService administrationService,
            ProjectTemplateRepository projectTemplateRepository,
            ProjectViewMapper viewMapper) {
        this.authorizationService = authorizationService;
        this.administrationService = administrationService;
        this.projectTemplateRepository = projectTemplateRepository;
        this.viewMapper = viewMapper;
    }

    public List<TemplateReadModels.ProjectTemplateListReadModel> execute(AuthenticatedUser actor) {
        authorizationService.assertEnabled();
        if (actor == null) {
            throw new org.springframework.security.access.AccessDeniedException("Authenticated user is required.");
        }
        return projectTemplateRepository.findAllByOrderByFrameworkTypeAscVersionDesc().stream()
                .filter(template -> administrationService.canUse(actor, template.ownerOrganizationId()))
                .map(viewMapper::toProjectTemplateListReadModel)
                .toList();
    }
}



