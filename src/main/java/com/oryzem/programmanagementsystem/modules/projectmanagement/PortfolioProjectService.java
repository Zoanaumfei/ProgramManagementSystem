package com.oryzem.programmanagementsystem.modules.projectmanagement;

import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import java.util.Comparator;
import org.springframework.stereotype.Service;

@Service
class PortfolioProjectService {

    private final ProgramRepository programRepository;
    private final ProjectRepository projectRepository;
    private final ProductRepository productRepository;
    private final ProjectManagementLookupService lookupService;
    private final ProjectManagementAccessService accessService;

    PortfolioProjectService(
            ProgramRepository programRepository,
            ProjectRepository projectRepository,
            ProductRepository productRepository,
            ProjectManagementLookupService lookupService,
            ProjectManagementAccessService accessService) {
        this.programRepository = programRepository;
        this.projectRepository = projectRepository;
        this.productRepository = productRepository;
        this.lookupService = lookupService;
        this.accessService = accessService;
    }

    ProjectResponse createProject(String programId, CreateProjectRequest request, AuthenticatedUser actor) {
        ProgramEntity program = lookupService.findProgram(programId);
        accessService.assertCanManageProjectLayer(actor, program.getOwnerOrganizationId());
        ProjectEntity project = buildProject(program, request, actor.username());
        program.addProject(project, actor.username());
        programRepository.save(program);
        return ProjectResponse.from(project);
    }

    ProductResponse createProduct(String projectId, CreateProductRequest request, AuthenticatedUser actor) {
        if (productRepository.existsByCodeIgnoreCase(request.code().trim())) {
            throw new IllegalArgumentException("Product code already exists.");
        }

        ProjectEntity project = lookupService.findProject(projectId);
        accessService.assertCanManageProjectLayer(actor, project.getProgram().getOwnerOrganizationId());
        ProductEntity product = ProductEntity.create(
                actor.username(),
                project,
                request.name().trim(),
                request.code().trim().toUpperCase(),
                ProjectManagementValidationSupport.trimToNull(request.description()),
                ProjectManagementValidationSupport.defaultValue(request.status(), ProductStatus.ACTIVE));
        project.addProduct(product, actor.username());
        projectRepository.save(project);
        return ProductResponse.from(product);
    }

    ProjectEntity buildProject(ProgramEntity program, CreateProjectRequest request, String actor) {
        ProjectManagementValidationSupport.validateDateRange(
                request.plannedStartDate(),
                request.plannedEndDate(),
                "Project");
        if (projectRepository.existsByCodeIgnoreCase(request.code().trim())) {
            throw new IllegalArgumentException("Project code already exists.");
        }

        MilestoneTemplateEntity template = null;
        if (request.milestoneTemplateId() != null && !request.milestoneTemplateId().isBlank()) {
            template = lookupService.findMilestoneTemplate(request.milestoneTemplateId());
        }

        ProjectEntity project = ProjectEntity.create(
                actor,
                program,
                request.name().trim(),
                request.code().trim().toUpperCase(),
                ProjectManagementValidationSupport.trimToNull(request.description()),
                ProjectManagementValidationSupport.defaultValue(request.status(), ProjectStatus.DRAFT),
                request.plannedStartDate(),
                request.plannedEndDate(),
                template != null ? template.getId() : null);

        if (template != null) {
            template.getItems().stream()
                    .sorted(Comparator.comparing(MilestoneTemplateItemEntity::getSortOrder))
                    .forEach(templateItem -> project.addMilestone(ProjectMilestoneEntity.create(
                            actor,
                            project,
                            templateItem.getId(),
                            templateItem.getName(),
                            templateItem.getSortOrder(),
                            ProjectMilestoneStatus.PLANNED,
                            request.plannedStartDate().plusWeeks(
                                    ProjectManagementValidationSupport.defaultValue(templateItem.getOffsetWeeks(), 0))), actor));
        }

        return project;
    }
}
