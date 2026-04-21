package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectPhaseTemplateAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectPhaseTemplateRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectTemplateRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectTemplateAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.support.ProjectIds;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.shared.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateProjectPhaseTemplateUseCase {

    private final ProjectAuthorizationService authorizationService;
    private final ProjectStructureTemplateAdministrationService administrationService;
    private final ProjectTemplateRepository projectTemplateRepository;
    private final ProjectPhaseTemplateRepository phaseTemplateRepository;
    private final ProjectViewMapper viewMapper;

    public CreateProjectPhaseTemplateUseCase(
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

    @Transactional
    public TemplateViews.ProjectPhaseTemplateView execute(String templateId, CreateProjectPhaseTemplateCommand command, AuthenticatedUser actor) {
        authorizationService.assertEnabled();
        ProjectTemplateAggregate template = projectTemplateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("ProjectTemplate", templateId));
        administrationService.authorizeManagement(actor, template.ownerOrganizationId());
        int nextSequence = phaseTemplateRepository.findAllByTemplateIdOrderBySequenceNoAsc(template.id()).size() + 1;
        ProjectPhaseTemplateAggregate entity = phaseTemplateRepository.save(new ProjectPhaseTemplateAggregate(
                ProjectIds.newProjectPhaseTemplateId(),
                template.id(),
                nextSequence,
                command.name().trim(),
                command.description(),
                command.plannedStartOffsetDays(),
                command.plannedEndOffsetDays()));
        return viewMapper.toPhaseTemplateView(entity);
    }

    public record CreateProjectPhaseTemplateCommand(
            String name,
            String description,
            Integer plannedStartOffsetDays,
            int plannedEndOffsetDays) {
    }
}


