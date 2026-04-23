package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectStructureTemplateAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectStructureTemplateRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.support.ProjectIds;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.shared.BusinessRuleException;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateProjectStructureTemplateUseCase {

    private final ProjectAuthorizationService authorizationService;
    private final ProjectStructureTemplateAdministrationService administrationService;
    private final ProjectFrameworkCatalogService projectFrameworkCatalogService;
    private final ProjectStructureTemplateRepository structureTemplateRepository;

    public CreateProjectStructureTemplateUseCase(
            ProjectAuthorizationService authorizationService,
            ProjectStructureTemplateAdministrationService administrationService,
            ProjectFrameworkCatalogService projectFrameworkCatalogService,
            ProjectStructureTemplateRepository structureTemplateRepository) {
        this.authorizationService = authorizationService;
        this.administrationService = administrationService;
        this.projectFrameworkCatalogService = projectFrameworkCatalogService;
        this.structureTemplateRepository = structureTemplateRepository;
    }

    @Transactional
    public StructureViews.ProjectStructureTemplateDetailView execute(CreateProjectStructureTemplateCommand command, AuthenticatedUser actor) {
        authorizationService.assertEnabled();
        administrationService.authorizeTemplateCreation(actor);
        projectFrameworkCatalogService.requireActiveFramework(command.frameworkType());
        boolean duplicate = structureTemplateRepository.findAllByOrderByFrameworkTypeAscVersionDescNameAsc().stream()
                .anyMatch(existing -> existing.frameworkType().equals(command.frameworkType())
                        && existing.version() == command.version()
                        && existing.ownerOrganizationId().equals(actor.organizationId())
                        && existing.name().equalsIgnoreCase(command.name().trim()));
        if (duplicate) {
            throw new BusinessRuleException("PROJECT_STRUCTURE_TEMPLATE_ALREADY_EXISTS", "A structure template with the same framework, name and version already exists.");
        }
        ProjectStructureTemplateAggregate entity = structureTemplateRepository.save(new ProjectStructureTemplateAggregate(
                ProjectIds.newProjectStructureTemplateId(),
                command.name().trim(),
                command.frameworkType(),
                command.version(),
                command.active(),
                actor.organizationId(),
                Instant.now()));
        return new StructureViews.ProjectStructureTemplateDetailView(
                entity.id(),
                entity.name(),
                entity.frameworkType(),
                entity.version(),
                entity.active(),
                java.util.List.of(),
                java.util.List.of(),
                java.util.List.of(),
                java.util.List.of());
    }

    public record CreateProjectStructureTemplateCommand(
            String name,
            String frameworkType,
            int version,
            boolean active) {
    }
}


