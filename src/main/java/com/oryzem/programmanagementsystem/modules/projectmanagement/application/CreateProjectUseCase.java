package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectFrameworkType;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectMemberAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectMemberRole;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectOrganizationAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectOrganizationRoleType;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectStatus;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectTemplateAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectTemplateStatus;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectVisibilityScope;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectMemberRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectOrganizationRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectTemplateRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.support.ProjectIds;
import java.time.Clock;
import com.oryzem.programmanagementsystem.platform.access.AccessContextService;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.shared.BusinessRuleException;
import com.oryzem.programmanagementsystem.platform.shared.ResourceNotFoundException;
import com.oryzem.programmanagementsystem.platform.tenant.OrganizationLookup;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateProjectUseCase {

    private static final String IDEMPOTENT_OPERATION = "CREATE_PROJECT";

    private final ProjectRepository projectRepository;
    private final ProjectOrganizationRepository organizationRepository;
    private final ProjectMemberRepository memberRepository;
    private final ProjectTemplateRepository templateRepository;
    private final OrganizationLookup organizationLookup;
    private final AccessContextService accessContextService;
    private final ProjectAuthorizationService authorizationService;
    private final InstantiateProjectFromTemplateUseCase instantiateProjectFromTemplateUseCase;
    private final ProjectViewMapper viewMapper;
    private final ProjectAuditService auditService;
    private final ProjectIdempotencyService idempotencyService;
    private final Clock clock;

    public CreateProjectUseCase(
            ProjectRepository projectRepository,
            ProjectOrganizationRepository organizationRepository,
            ProjectMemberRepository memberRepository,
            ProjectTemplateRepository templateRepository,
            OrganizationLookup organizationLookup,
            AccessContextService accessContextService,
            ProjectAuthorizationService authorizationService,
            InstantiateProjectFromTemplateUseCase instantiateProjectFromTemplateUseCase,
            ProjectViewMapper viewMapper,
            ProjectAuditService auditService,
            ProjectIdempotencyService idempotencyService,
            Clock clock) {
        this.projectRepository = projectRepository;
        this.organizationRepository = organizationRepository;
        this.memberRepository = memberRepository;
        this.templateRepository = templateRepository;
        this.organizationLookup = organizationLookup;
        this.accessContextService = accessContextService;
        this.authorizationService = authorizationService;
        this.instantiateProjectFromTemplateUseCase = instantiateProjectFromTemplateUseCase;
        this.viewMapper = viewMapper;
        this.auditService = auditService;
        this.idempotencyService = idempotencyService;
        this.clock = clock;
    }

    @Transactional
    public ProjectViews.ProjectDetailView execute(CreateProjectCommand command, AuthenticatedUser actor, String idempotencyKey) {
        authorizationService.assertEnabled();
        if (actor == null || actor.tenantId() == null || actor.organizationId() == null) {
            throw new BusinessRuleException("PROJECT_CONTEXT_REQUIRED", "An authenticated tenant and organization context are required.");
        }
        return idempotencyService.execute(actor.tenantId(), IDEMPOTENT_OPERATION, idempotencyKey, command, ProjectViews.ProjectDetailView.class, () -> doCreate(command, actor));
    }

    private ProjectViews.ProjectDetailView doCreate(CreateProjectCommand command, AuthenticatedUser actor) {
        String tenantId = accessContextService.canonicalTenantId(actor.tenantId());
        if (projectRepository.existsByTenantIdAndCodeIgnoreCase(tenantId, command.code())) {
            throw new BusinessRuleException("PROJECT_CODE_ALREADY_EXISTS", "Project code already exists for the active tenant.");
        }
        organizationLookup.findById(actor.organizationId())
                .filter(OrganizationLookup.OrganizationView::active)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", actor.organizationId()));
        if (command.customerOrganizationId() != null) {
            OrganizationLookup.OrganizationView customer = organizationLookup.findById(command.customerOrganizationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Organization", command.customerOrganizationId()));
            if (!customer.active() || !tenantId.equals(customer.tenantId())) {
                throw new BusinessRuleException("PROJECT_CUSTOMER_ORGANIZATION_INVALID", "Customer organization must be active and belong to the same tenant.");
            }
        }
        ProjectTemplateAggregate template = resolveTemplate(command);
        Instant now = Instant.now(clock);
        ProjectAggregate aggregate = new ProjectAggregate(
                ProjectIds.newProjectId(),
                tenantId,
                command.code().trim(),
                command.name().trim(),
                command.description(),
                command.frameworkType(),
                template.id(),
                template.version(),
                actor.organizationId(),
                command.customerOrganizationId(),
                command.status() != null ? command.status() : ProjectStatus.DRAFT,
                command.visibilityScope() != null ? command.visibilityScope() : ProjectVisibilityScope.ALL_PROJECT_PARTICIPANTS,
                command.plannedStartDate(),
                command.plannedEndDate(),
                null,
                null,
                actor.userId(),
                now,
                now,
                0L);
        ProjectAggregate project = projectRepository.save(aggregate);
        List<ProjectOrganizationAggregate> organizations = new ArrayList<>();
        organizations.add(organizationRepository.save(new ProjectOrganizationAggregate(
                ProjectIds.newProjectOrganizationId(),
                project.id(),
                actor.organizationId(),
                ProjectOrganizationRoleType.LEAD,
                now,
                true)));
        if (command.customerOrganizationId() != null && !command.customerOrganizationId().equals(actor.organizationId())) {
            organizations.add(organizationRepository.save(new ProjectOrganizationAggregate(
                    ProjectIds.newProjectOrganizationId(),
                    project.id(),
                    command.customerOrganizationId(),
                    ProjectOrganizationRoleType.CUSTOMER,
                    now,
                    true)));
        }
        List<ProjectMemberAggregate> members = List.of(memberRepository.save(new ProjectMemberAggregate(
                ProjectIds.newProjectMemberId(),
                project.id(),
                actor.userId(),
                actor.organizationId(),
                ProjectMemberRole.PROJECT_MANAGER,
                true,
                now)));
        instantiateProjectFromTemplateUseCase.execute(project, organizations);
        auditService.record(actor, "PROJECT_CREATED", project.tenantId(), project.id(), "PROJECT", new LinkedHashMap<>(java.util.Map.of("code", project.code(), "frameworkType", project.frameworkType().name(), "templateId", project.templateId(), "templateVersion", project.templateVersion())));
        return viewMapper.toDetail(project, organizations, members);
    }

    private ProjectTemplateAggregate resolveTemplate(CreateProjectCommand command) {
        if (command.templateId() != null && !command.templateId().isBlank()) {
            ProjectTemplateAggregate template = templateRepository.findById(command.templateId())
                    .orElseThrow(() -> new ResourceNotFoundException("ProjectTemplate", command.templateId()));
            if (template.frameworkType() != command.frameworkType()) {
                throw new BusinessRuleException("PROJECT_TEMPLATE_FRAMEWORK_MISMATCH", "Project template framework must match the project framework.");
            }
            return template;
        }
        return templateRepository.findByFrameworkTypeAndIsDefaultTrueAndStatus(command.frameworkType(), ProjectTemplateStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("ProjectTemplate", command.frameworkType().name() + ":default"));
    }

    public record CreateProjectCommand(
            String code,
            String name,
            String description,
            ProjectFrameworkType frameworkType,
            String templateId,
            String customerOrganizationId,
            ProjectStatus status,
            ProjectVisibilityScope visibilityScope,
            LocalDate plannedStartDate,
            LocalDate plannedEndDate) {
    }
}

