package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectMilestoneTemplateRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectTemplateRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectTemplateAggregate;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.shared.ResourceNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ListProjectMilestoneTemplatesUseCase {

    private final ProjectAuthorizationService authorizationService;
    private final ProjectStructureTemplateAdministrationService administrationService;
    private final ProjectTemplateRepository projectTemplateRepository;
    private final ProjectMilestoneTemplateRepository milestoneTemplateRepository;
    private final ProjectViewMapper viewMapper;

    public ListProjectMilestoneTemplatesUseCase(
            ProjectAuthorizationService authorizationService,
            ProjectStructureTemplateAdministrationService administrationService,
            ProjectTemplateRepository projectTemplateRepository,
            ProjectMilestoneTemplateRepository milestoneTemplateRepository,
            ProjectViewMapper viewMapper) {
        this.authorizationService = authorizationService;
        this.administrationService = administrationService;
        this.projectTemplateRepository = projectTemplateRepository;
        this.milestoneTemplateRepository = milestoneTemplateRepository;
        this.viewMapper = viewMapper;
    }

    public List<TemplateViews.ProjectTemplateMilestoneTemplateView> execute(String templateId, AuthenticatedUser actor) {
        authorizationService.assertEnabled();
        ProjectTemplateAggregate template = projectTemplateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("ProjectTemplate", templateId));
        administrationService.authorizeUse(actor, template.ownerOrganizationId());
        return milestoneTemplateRepository.findAllByTemplateIdOrderBySequenceNoAsc(template.id()).stream()
                .map(viewMapper::toMilestoneTemplateView)
                .toList();
    }
}


