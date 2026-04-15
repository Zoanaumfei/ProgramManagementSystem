package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectPhaseTemplateAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectPhaseTemplateRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectTemplateRepository;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.shared.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateProjectPhaseTemplateUseCase {

    private final ProjectAuthorizationService authorizationService;
    private final ProjectStructureTemplateAdministrationService administrationService;
    private final ProjectTemplateRepository projectTemplateRepository;
    private final ProjectPhaseTemplateRepository phaseTemplateRepository;
    private final ProjectViewMapper viewMapper;

    public UpdateProjectPhaseTemplateUseCase(
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
    public TemplateViews.ProjectPhaseTemplateView execute(String templateId, String phaseTemplateId, UpdateProjectPhaseTemplateCommand command, AuthenticatedUser actor) {
        authorizationService.assertEnabled();
        administrationService.authorizeManagement(actor);
        projectTemplateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("ProjectTemplate", templateId));
        ProjectPhaseTemplateAggregate entity = phaseTemplateRepository.findById(phaseTemplateId)
                .filter(phase -> phase.templateId().equals(templateId))
                .orElseThrow(() -> new ResourceNotFoundException("ProjectPhaseTemplate", phaseTemplateId));
        entity = entity.update(
                command.name().trim(),
                command.description(),
                command.plannedStartOffsetDays(),
                command.plannedEndOffsetDays());
        entity = phaseTemplateRepository.save(entity);
        return viewMapper.toPhaseTemplateView(entity);
    }

    public record UpdateProjectPhaseTemplateCommand(
            String name,
            String description,
            Integer plannedStartOffsetDays,
            int plannedEndOffsetDays) {
    }
}


