package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.model.read.StructureReadModels;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectStructureTemplateAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectStructureTemplateRepository;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.shared.BusinessRuleException;
import com.oryzem.programmanagementsystem.platform.shared.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateProjectStructureTemplateUseCase {

    private final ProjectAuthorizationService authorizationService;
    private final ProjectStructureTemplateAdministrationService administrationService;
    private final ProjectStructureTemplateRepository structureTemplateRepository;
    private final GetProjectStructureTemplateDetailUseCase getProjectStructureTemplateDetailUseCase;

    public UpdateProjectStructureTemplateUseCase(
            ProjectAuthorizationService authorizationService,
            ProjectStructureTemplateAdministrationService administrationService,
            ProjectStructureTemplateRepository structureTemplateRepository,
            GetProjectStructureTemplateDetailUseCase getProjectStructureTemplateDetailUseCase) {
        this.authorizationService = authorizationService;
        this.administrationService = administrationService;
        this.structureTemplateRepository = structureTemplateRepository;
        this.getProjectStructureTemplateDetailUseCase = getProjectStructureTemplateDetailUseCase;
    }

    @Transactional
    public StructureReadModels.ProjectStructureTemplateDetailReadModel execute(String structureTemplateId, UpdateProjectStructureTemplateCommand command, AuthenticatedUser actor) {
        authorizationService.assertEnabled();
        administrationService.authorizeManagement(actor);
        ProjectStructureTemplateAggregate entity = structureTemplateRepository.findById(structureTemplateId)
                .orElseThrow(() -> new ResourceNotFoundException("ProjectStructureTemplate", structureTemplateId));
        boolean duplicate = structureTemplateRepository.findAllByOrderByFrameworkTypeAscVersionDescNameAsc().stream()
                .anyMatch(existing -> !existing.id().equals(structureTemplateId)
                        && existing.frameworkType() == entity.frameworkType()
                        && existing.version() == entity.version()
                        && existing.name().equalsIgnoreCase(command.name().trim()));
        if (duplicate) {
            throw new BusinessRuleException("PROJECT_STRUCTURE_TEMPLATE_ALREADY_EXISTS", "A structure template with the same framework, name and version already exists.");
        }
        structureTemplateRepository.save(entity.rename(command.name().trim()));
        return getProjectStructureTemplateDetailUseCase.execute(structureTemplateId, actor);
    }

    public record UpdateProjectStructureTemplateCommand(String name) {
    }
}


