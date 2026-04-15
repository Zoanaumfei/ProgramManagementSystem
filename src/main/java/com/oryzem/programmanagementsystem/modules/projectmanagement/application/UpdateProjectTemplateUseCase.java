package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectTemplateStatus;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectStructureTemplateAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectTemplateAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectStructureTemplateRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectTemplateRepository;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.shared.BusinessRuleException;
import com.oryzem.programmanagementsystem.platform.shared.ResourceNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateProjectTemplateUseCase {

    private final ProjectAuthorizationService authorizationService;
    private final ProjectStructureTemplateAdministrationService administrationService;
    private final ProjectTemplateRepository projectTemplateRepository;
    private final ProjectStructureTemplateRepository structureTemplateRepository;
    private final ProjectViewMapper viewMapper;

    public UpdateProjectTemplateUseCase(
            ProjectAuthorizationService authorizationService,
            ProjectStructureTemplateAdministrationService administrationService,
            ProjectTemplateRepository projectTemplateRepository,
            ProjectStructureTemplateRepository structureTemplateRepository,
            ProjectViewMapper viewMapper) {
        this.authorizationService = authorizationService;
        this.administrationService = administrationService;
        this.projectTemplateRepository = projectTemplateRepository;
        this.structureTemplateRepository = structureTemplateRepository;
        this.viewMapper = viewMapper;
    }

    @Transactional
    public TemplateViews.ProjectTemplateDetailView execute(String templateId, UpdateProjectTemplateCommand command, AuthenticatedUser actor) {
        authorizationService.assertEnabled();
        administrationService.authorizeManagement(actor);
        ProjectTemplateAggregate entity = projectTemplateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("ProjectTemplate", templateId));
        ProjectStructureTemplateAggregate structureTemplate = structureTemplateRepository.findById(command.structureTemplateId())
                .orElseThrow(() -> new ResourceNotFoundException("ProjectStructureTemplate", command.structureTemplateId()));
        if (structureTemplate.frameworkType() != entity.frameworkType()) {
            throw new BusinessRuleException("PROJECT_TEMPLATE_STRUCTURE_FRAMEWORK_MISMATCH", "Project template framework must match the linked structure template framework.");
        }
        if (command.isDefault() && command.status() != ProjectTemplateStatus.ACTIVE) {
            throw new BusinessRuleException("PROJECT_TEMPLATE_DEFAULT_MUST_BE_ACTIVE", "Default project templates must be active.");
        }
        List<ProjectTemplateAggregate> existing = projectTemplateRepository.findAllByOrderByFrameworkTypeAscVersionDesc();
        ProjectTemplateAggregate current = entity;
        if (command.isDefault()) {
            projectTemplateRepository.saveAll(existing.stream()
                    .map(template -> template.frameworkType() == current.frameworkType()
                            && template.isDefault()
                            && !template.id().equals(templateId)
                            ? template.withDefault(false)
                            : template)
                    .toList());
        }
        entity = entity.update(
                command.name().trim(),
                command.status(),
                command.structureTemplateId(),
                command.isDefault());
        projectTemplateRepository.save(entity);
        return viewMapper.toProjectTemplateDetailView(entity);
    }

    public record UpdateProjectTemplateCommand(
            String name,
            ProjectTemplateStatus status,
            boolean isDefault,
            String structureTemplateId) {
    }
}


