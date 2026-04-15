package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectPhaseTemplateRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectTemplateRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectTemplateAggregate;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.shared.ResourceNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ListProjectPhaseTemplatesUseCase {

    private final ProjectAuthorizationService authorizationService;
    private final ProjectStructureTemplateAdministrationService administrationService;
    private final ProjectTemplateRepository projectTemplateRepository;
    private final ProjectPhaseTemplateRepository phaseTemplateRepository;
    private final ProjectViewMapper viewMapper;

    public ListProjectPhaseTemplatesUseCase(
            ProjectAuthorizationService authorizationService,
            ProjectStructureTemplateAdministrationService administrationService,
            ProjectTemplateRepository projectTemplateRepository,
            ProjectPhaseTemplateRepository phaseTemplateRepository,
            ProjectViewMapper viewMapper) {
        this.authorizationService = authorizationService;
        this.administrationService = administrationService;
        this.projectTemplateRepository = projectTemplateRepository;
        this.phaseTemplateRepository = phaseTemplateRepository;
        this.viewMapper = viewMapper;
    }

    public List<TemplateViews.ProjectPhaseTemplateView> execute(String templateId, AuthenticatedUser actor) {
        authorizationService.assertEnabled();
        administrationService.authorizeManagement(actor);
        ProjectTemplateAggregate template = projectTemplateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("ProjectTemplate", templateId));
        return phaseTemplateRepository.findAllByTemplateIdOrderBySequenceNoAsc(template.id()).stream()
                .map(viewMapper::toPhaseTemplateView)
                .toList();
    }
}


