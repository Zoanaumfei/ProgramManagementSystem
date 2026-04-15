package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectTemplateStatus;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectStructureTemplateAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectTemplateAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectStructureTemplateRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectTemplateRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.support.ProjectIds;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.shared.BusinessRuleException;
import com.oryzem.programmanagementsystem.platform.shared.ResourceNotFoundException;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateProjectTemplateUseCase {

    private final ProjectAuthorizationService authorizationService;
    private final ProjectStructureTemplateAdministrationService administrationService;
    private final ProjectTemplateRepository projectTemplateRepository;
    private final ProjectStructureTemplateRepository structureTemplateRepository;
    private final ProjectViewMapper viewMapper;

    public CreateProjectTemplateUseCase(
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
    public TemplateViews.ProjectTemplateDetailView execute(CreateProjectTemplateCommand command, AuthenticatedUser actor) {
        authorizationService.assertEnabled();
        administrationService.authorizeManagement(actor);
        ProjectStructureTemplateAggregate structureTemplate = structureTemplateRepository.findById(command.structureTemplateId())
                .orElseThrow(() -> new ResourceNotFoundException("ProjectStructureTemplate", command.structureTemplateId()));
        if (structureTemplate.frameworkType() != command.frameworkType()) {
            throw new BusinessRuleException("PROJECT_TEMPLATE_STRUCTURE_FRAMEWORK_MISMATCH", "Project template framework must match the linked structure template framework.");
        }
        if (command.isDefault() && command.status() != ProjectTemplateStatus.ACTIVE) {
            throw new BusinessRuleException("PROJECT_TEMPLATE_DEFAULT_MUST_BE_ACTIVE", "Default project templates must be active.");
        }
        List<ProjectTemplateAggregate> existing = projectTemplateRepository.findAllByOrderByFrameworkTypeAscVersionDesc();
        if (existing.stream().anyMatch(template -> template.frameworkType() == command.frameworkType() && template.version() == command.version())) {
            throw new BusinessRuleException("PROJECT_TEMPLATE_VERSION_ALREADY_EXISTS", "A project template already exists for this framework and version.");
        }
        if (command.isDefault()) {
            projectTemplateRepository.saveAll(existing.stream()
                    .map(template -> template.frameworkType() == command.frameworkType() && template.isDefault()
                            ? template.withDefault(false)
                            : template)
                    .toList());
        }
        ProjectTemplateAggregate entity = projectTemplateRepository.save(new ProjectTemplateAggregate(
                ProjectIds.newProjectTemplateId(),
                command.name().trim(),
                command.frameworkType(),
                command.version(),
                command.status(),
                command.structureTemplateId(),
                command.isDefault(),
                Instant.now()));
        return viewMapper.toProjectTemplateDetailView(entity);
    }

    public record CreateProjectTemplateCommand(
            String name,
            com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectFrameworkType frameworkType,
            int version,
            ProjectTemplateStatus status,
            boolean isDefault,
            String structureTemplateId) {
    }
}


