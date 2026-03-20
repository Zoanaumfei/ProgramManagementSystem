package com.oryzem.programmanagementsystem.portfolio;

import com.oryzem.programmanagementsystem.authorization.Action;
import com.oryzem.programmanagementsystem.authorization.AppModule;
import com.oryzem.programmanagementsystem.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.authorization.AuthorizationContext;
import com.oryzem.programmanagementsystem.authorization.AuthorizationDecision;
import com.oryzem.programmanagementsystem.authorization.AuthorizationService;
import com.oryzem.programmanagementsystem.authorization.TenantType;
import com.oryzem.programmanagementsystem.authorization.Role;
import com.oryzem.programmanagementsystem.users.ManagedUser;
import com.oryzem.programmanagementsystem.users.UserRepository;
import com.oryzem.programmanagementsystem.users.UserStatus;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PortfolioManagementService {

    private final OrganizationRepository organizationRepository;
    private final ProgramRepository programRepository;
    private final ProjectRepository projectRepository;
    private final ProductRepository productRepository;
    private final ItemRepository itemRepository;
    private final DeliverableRepository deliverableRepository;
    private final DeliverableDocumentRepository deliverableDocumentRepository;
    private final MilestoneTemplateRepository milestoneTemplateRepository;
    private final UserRepository userRepository;
    private final AuthorizationService authorizationService;
    private final OrganizationDirectoryService organizationDirectoryService;
    private final PortfolioDocumentStorageGateway documentStorageGateway;
    private final PortfolioDocumentProperties documentProperties;

    public PortfolioManagementService(
            OrganizationRepository organizationRepository,
            ProgramRepository programRepository,
            ProjectRepository projectRepository,
            ProductRepository productRepository,
            ItemRepository itemRepository,
            DeliverableRepository deliverableRepository,
            DeliverableDocumentRepository deliverableDocumentRepository,
            MilestoneTemplateRepository milestoneTemplateRepository,
            UserRepository userRepository,
            AuthorizationService authorizationService,
            OrganizationDirectoryService organizationDirectoryService,
            PortfolioDocumentStorageGateway documentStorageGateway,
            PortfolioDocumentProperties documentProperties) {
        this.organizationRepository = organizationRepository;
        this.programRepository = programRepository;
        this.projectRepository = projectRepository;
        this.productRepository = productRepository;
        this.itemRepository = itemRepository;
        this.deliverableRepository = deliverableRepository;
        this.deliverableDocumentRepository = deliverableDocumentRepository;
        this.milestoneTemplateRepository = milestoneTemplateRepository;
        this.userRepository = userRepository;
        this.authorizationService = authorizationService;
        this.organizationDirectoryService = organizationDirectoryService;
        this.documentStorageGateway = documentStorageGateway;
        this.documentProperties = documentProperties;
    }

    @Transactional(readOnly = true)
    public List<OrganizationResponse> listOrganizations(
            AuthenticatedUser actor,
            OrganizationStatus status,
            OrganizationSetupStatus setupStatus,
            String customerOrganizationId,
            String parentOrganizationId,
            Integer hierarchyLevel,
            String search) {
        assertAllowed(authorizationService.decide(
                actor,
                AuthorizationContext.builder(AppModule.TENANT, Action.VIEW)
                        .build()));
        List<OrganizationEntity> organizations = visibleOrganizations(actor);
        OrganizationManagementSnapshot snapshot = buildOrganizationManagementSnapshot(organizations);
        String normalizedCustomerOrganizationId = trimToNull(customerOrganizationId);
        String normalizedParentOrganizationId = trimToNull(parentOrganizationId);

        return organizations.stream()
                .map(organization -> toOrganizationResponse(organization, snapshot))
                .filter(organization -> status == null || organization.status() == status)
                .filter(organization -> setupStatus == null || organization.setupStatus() == setupStatus)
                .filter(organization -> normalizedCustomerOrganizationId == null
                        || normalizedCustomerOrganizationId.equals(organization.customerOrganizationId()))
                .filter(organization -> normalizedParentOrganizationId == null
                        || normalizedParentOrganizationId.equals(organization.parentOrganizationId()))
                .filter(organization -> hierarchyLevel == null || hierarchyLevel.equals(organization.hierarchyLevel()))
                .filter(organization -> matchesSearch(organization, search))
                .toList();
    }

    @Transactional(readOnly = true)
    public OrganizationResponse getOrganization(String organizationId, AuthenticatedUser actor) {
        OrganizationEntity organization = findPortfolioOrganization(organizationId);
        assertCanAccessOrganization(actor, organization, Action.VIEW);
        assertCanViewOrganization(actor, organization);
        return toOrganizationResponse(
                organization,
                buildOrganizationManagementSnapshot(List.of(organization)));
    }

    public OrganizationResponse createOrganization(CreateOrganizationRequest request, AuthenticatedUser actor) {
        String normalizedCode = normalizeCode(request.code());
        if (organizationRepository.existsByCodeIgnoreCase(normalizedCode)) {
            throw new IllegalArgumentException("Organization code already exists.");
        }

        OrganizationEntity organization;
        String parentOrganizationId = trimToNull(request.parentOrganizationId());
        if (parentOrganizationId == null) {
            assertAllowed(authorizationService.decide(
                    actor,
                    AuthorizationContext.builder(AppModule.TENANT, Action.CREATE)
                            .build()));
            assertCanCreateRootCustomer(actor);
            organization = OrganizationEntity.createRootExternal(
                    actor.username(),
                    normalizeName(request.name()),
                    normalizedCode,
                    defaultValue(request.status(), OrganizationStatus.ACTIVE));
        } else {
            OrganizationEntity parentOrganization = findPortfolioOrganization(parentOrganizationId);
            assertCanAccessOrganization(actor, parentOrganization, Action.CREATE);
            assertCanCreateChildOrganization(actor, parentOrganization);
            assertOrganizationCanOwnChildren(parentOrganization);
            organization = OrganizationEntity.createChild(
                    actor.username(),
                    normalizeName(request.name()),
                    normalizedCode,
                    defaultValue(request.status(), OrganizationStatus.ACTIVE),
                    parentOrganization);
        }
        return toOrganizationResponse(organizationRepository.save(organization));
    }

    public OrganizationResponse updateOrganization(
            String organizationId,
            UpdateOrganizationRequest request,
            AuthenticatedUser actor) {
        OrganizationEntity organization = findPortfolioOrganization(organizationId);
        assertCanAccessOrganization(actor, organization, Action.EDIT);
        assertCanManageOrganization(actor, organization);
        ensureOrganizationIsMutable(organization);

        String normalizedCode = normalizeCode(request.code());
        if (organizationRepository.existsByCodeIgnoreCaseAndIdNot(normalizedCode, organization.getId())) {
            throw new IllegalArgumentException("Organization code already exists.");
        }

        organization.updateDetails(actor.username(), normalizeName(request.name()), normalizedCode);
        return toOrganizationResponse(organizationRepository.save(organization));
    }

    public OrganizationResponse inactivateOrganization(String organizationId, AuthenticatedUser actor) {
        OrganizationEntity organization = findPortfolioOrganization(organizationId);
        assertCanAccessOrganization(actor, organization, Action.DELETE);
        assertCanManageOrganization(actor, organization);
        if (organization.getStatus() == OrganizationStatus.INACTIVE) {
            return toOrganizationResponse(organization);
        }

        ensureOrganizationHasNoInvitedOrActiveUsers(organization.getId());
        ensureOrganizationHasNoActiveChildren(organization.getId());
        ensureOrganizationHasNoActiveProjects(organization.getId());
        organization.markInactive(actor.username());
        return toOrganizationResponse(organizationRepository.save(organization));
    }

    @Transactional(readOnly = true)
    public List<MilestoneTemplateResponse> listMilestoneTemplates(AuthenticatedUser actor) {
        assertCanViewPortfolio(actor, null);
        return milestoneTemplateRepository.findAllByOrderByNameAsc().stream()
                .map(MilestoneTemplateResponse::from)
                .toList();
    }

    public MilestoneTemplateResponse createMilestoneTemplate(
            CreateMilestoneTemplateRequest request,
            AuthenticatedUser actor) {
        assertCanConfigurePortfolio(actor);
        MilestoneTemplateEntity template = MilestoneTemplateEntity.create(
                actor.username(),
                request.name().trim(),
                trimToNull(request.description()),
                defaultValue(request.status(), MilestoneTemplateStatus.ACTIVE));

        request.items().stream()
                .sorted(java.util.Comparator.comparing(CreateMilestoneTemplateItemRequest::sortOrder))
                .forEach(itemRequest -> template.addItem(MilestoneTemplateItemEntity.create(
                        actor.username(),
                        template,
                        itemRequest.name().trim(),
                        itemRequest.sortOrder(),
                        itemRequest.required(),
                        itemRequest.offsetWeeks()), actor.username()));

        return MilestoneTemplateResponse.from(milestoneTemplateRepository.save(template));
    }

    @Transactional(readOnly = true)
    public List<ProgramSummaryResponse> listPrograms(AuthenticatedUser actor, String ownerOrganizationId) {
        assertCanViewPortfolio(actor, null);
        String normalizedOwnerOrganizationId = trimToNull(ownerOrganizationId);
        if (normalizedOwnerOrganizationId != null) {
            OrganizationEntity ownerOrganization = findPortfolioOrganization(normalizedOwnerOrganizationId);
            assertCanViewPortfolio(actor, ownerOrganization.getId());
        }

        return visiblePrograms(actor).stream()
                .filter(program -> normalizedOwnerOrganizationId == null
                        || normalizedOwnerOrganizationId.equals(program.getOwnerOrganization().getId()))
                .map(ProgramSummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProgramDetailResponse getProgram(String programId, AuthenticatedUser actor) {
        ProgramEntity program = findProgram(programId);
        assertCanViewProgram(actor, program);
        return ProgramDetailResponse.from(program);
    }

    public ProgramDetailResponse createProgram(CreateProgramRequest request, AuthenticatedUser actor) {
        validateDateRange(request.plannedStartDate(), request.plannedEndDate(), "Program");
        if (programRepository.existsByCodeIgnoreCase(request.code().trim())) {
            throw new IllegalArgumentException("Program code already exists.");
        }
        if (request.initialProject() == null) {
            throw new IllegalArgumentException("Program requires an initial project.");
        }

        OrganizationEntity ownerOrganization = findPortfolioOrganization(request.ownerOrganizationId());
        assertCanManageProgram(actor, ownerOrganization.getId());
        assertPortfolioOrganization(ownerOrganization, "Owner organization");
        assertOrganizationIsActive(ownerOrganization, "Owner organization");
        assertOrganizationSetupComplete(ownerOrganization.getId(), "Owner organization");
        ProgramEntity program = ProgramEntity.create(
                actor.username(),
                request.name().trim(),
                request.code().trim().toUpperCase(),
                trimToNull(request.description()),
                ProgramStatus.DRAFT,
                request.plannedStartDate(),
                request.plannedEndDate(),
                ownerOrganization);

        addParticipants(program, ownerOrganization, request.participants(), actor.username());
        ProjectEntity initialProject = buildProject(program, request.initialProject(), actor.username());
        program.addProject(initialProject, actor.username());

        return ProgramDetailResponse.from(programRepository.save(program));
    }

    public ProjectResponse createProject(String programId, CreateProjectRequest request, AuthenticatedUser actor) {
        ProgramEntity program = findProgram(programId);
        assertCanManageProjectLayer(actor, program.getOwnerOrganization().getId());
        ProjectEntity project = buildProject(program, request, actor.username());
        program.addProject(project, actor.username());
        programRepository.save(program);
        return ProjectResponse.from(project);
    }

    public ProductResponse createProduct(String projectId, CreateProductRequest request, AuthenticatedUser actor) {
        if (productRepository.existsByCodeIgnoreCase(request.code().trim())) {
            throw new IllegalArgumentException("Product code already exists.");
        }

        ProjectEntity project = findProject(projectId);
        assertCanManageProjectLayer(actor, project.getProgram().getOwnerOrganization().getId());
        ProductEntity product = ProductEntity.create(
                actor.username(),
                project,
                request.name().trim(),
                request.code().trim().toUpperCase(),
                trimToNull(request.description()),
                defaultValue(request.status(), ProductStatus.ACTIVE));
        project.addProduct(product, actor.username());
        projectRepository.save(project);
        return ProductResponse.from(product);
    }

    public ItemResponse createItem(String productId, CreateItemRequest request, AuthenticatedUser actor) {
        if (itemRepository.existsByCodeIgnoreCase(request.code().trim())) {
            throw new IllegalArgumentException("Item code already exists.");
        }

        ProductEntity product = findProduct(productId);
        assertCanManageTaskLayer(actor, product.getProject().getProgram().getOwnerOrganization().getId());
        ItemEntity item = ItemEntity.create(
                actor.username(),
                product,
                request.name().trim(),
                request.code().trim().toUpperCase(),
                trimToNull(request.description()),
                defaultValue(request.status(), ItemStatus.ACTIVE));
        product.addItem(item, actor.username());
        productRepository.save(product);
        return ItemResponse.from(item);
    }

    public DeliverableResponse createDeliverable(String itemId, CreateDeliverableRequest request, AuthenticatedUser actor) {
        validateDateRange(request.plannedDate(), request.dueDate(), "Deliverable");

        ItemEntity item = findItem(itemId);
        assertCanManageTaskLayer(actor, item.getProduct().getProject().getProgram().getOwnerOrganization().getId());
        DeliverableEntity deliverable = DeliverableEntity.create(
                actor.username(),
                item,
                request.name().trim(),
                trimToNull(request.description()),
                request.type(),
                defaultValue(request.status(), DeliverableStatus.PENDING),
                request.plannedDate(),
                request.dueDate());
        item.addDeliverable(deliverable, actor.username());
        itemRepository.save(item);
        return DeliverableResponse.from(deliverable);
    }

    public OpenIssueResponse createOpenIssue(String programId, CreateOpenIssueRequest request, AuthenticatedUser actor) {
        ProgramEntity program = findProgram(programId);
        assertCanManageProjectLayer(actor, program.getOwnerOrganization().getId());
        OpenIssueEntity issue = OpenIssueEntity.create(
                actor.username(),
                program,
                request.title().trim(),
                trimToNull(request.description()),
                defaultValue(request.status(), OpenIssueStatus.OPEN),
                request.severity(),
                request.openedAt() != null ? request.openedAt() : OffsetDateTime.now());
        program.addOpenIssue(issue, actor.username());
        programRepository.save(program);
        return OpenIssueResponse.from(issue);
    }

    @Transactional(readOnly = true)
    public List<DeliverableDocumentResponse> listDeliverableDocuments(String deliverableId, AuthenticatedUser actor) {
        DeliverableEntity deliverable = findDeliverable(deliverableId);
        assertCanViewProgram(actor, deliverable.getItem().getProduct().getProject().getProgram());
        return deliverableDocumentRepository.findByDeliverableIdOrderByCreatedAtAsc(deliverableId).stream()
                .map(DeliverableDocumentResponse::from)
                .toList();
    }

    public DeliverableDocumentUploadResponse prepareDeliverableDocumentUpload(
            String deliverableId,
            PrepareDeliverableDocumentUploadRequest request,
            AuthenticatedUser actor) {
        DeliverableEntity deliverable = findDeliverable(deliverableId);
        assertCanManageTaskLayer(actor, deliverable.getItem().getProduct().getProject().getProgram().getOwnerOrganization().getId());
        assertDocumentDeliverable(deliverable);

        DeliverableDocumentEntity document = DeliverableDocumentEntity.createPendingUpload(
                actor.username(),
                deliverable,
                request.fileName().trim(),
                request.contentType().trim(),
                request.fileSize(),
                resolveBucketName(),
                buildStorageKey(deliverable, request.fileName()));
        deliverable.addDocument(document, actor.username());
        deliverableRepository.save(deliverable);

        PreparedDocumentUpload preparedUpload = documentStorageGateway.prepareUpload(document);
        return new DeliverableDocumentUploadResponse(
                DeliverableDocumentResponse.from(document),
                preparedUpload.uploadUrl(),
                preparedUpload.expiresAt(),
                preparedUpload.requiredHeaders());
    }

    public DeliverableDocumentResponse completeDeliverableDocumentUpload(
            String deliverableId,
            String documentId,
            AuthenticatedUser actor) {
        DeliverableDocumentEntity document = findDeliverableDocument(deliverableId, documentId);
        assertCanManageTaskLayer(
                actor,
                document.getDeliverable().getItem().getProduct().getProject().getProgram().getOwnerOrganization().getId());
        if (document.getStatus() == DeliverableDocumentStatus.DELETED) {
            throw new IllegalArgumentException("Deleted document cannot be completed.");
        }

        documentStorageGateway.assertObjectExists(document);
        document.markAvailable(actor.username());
        return DeliverableDocumentResponse.from(deliverableDocumentRepository.save(document));
    }

    public DeliverableDocumentDownloadResponse createDeliverableDocumentDownload(
            String deliverableId,
            String documentId,
            AuthenticatedUser actor) {
        DeliverableDocumentEntity document = findDeliverableDocument(deliverableId, documentId);
        assertCanViewProgram(actor, document.getDeliverable().getItem().getProduct().getProject().getProgram());
        if (document.getStatus() != DeliverableDocumentStatus.AVAILABLE) {
            throw new IllegalArgumentException("Only available documents can generate download URLs.");
        }

        PreparedDocumentDownload preparedDownload = documentStorageGateway.prepareDownload(document);
        return new DeliverableDocumentDownloadResponse(
                document.getId(),
                preparedDownload.downloadUrl(),
                preparedDownload.expiresAt());
    }

    public DeliverableDocumentResponse deleteDeliverableDocument(
            String deliverableId,
            String documentId,
            AuthenticatedUser actor) {
        DeliverableDocumentEntity document = findDeliverableDocument(deliverableId, documentId);
        assertCanManageTaskLayer(
                actor,
                document.getDeliverable().getItem().getProduct().getProject().getProgram().getOwnerOrganization().getId());
        document.markDeleted(actor.username());
        return DeliverableDocumentResponse.from(deliverableDocumentRepository.save(document));
    }

    private void addParticipants(
            ProgramEntity program,
            OrganizationEntity ownerOrganization,
            List<CreateProgramParticipationRequest> requests,
            String actor) {
        Set<String> includedOrganizations = new LinkedHashSet<>();
        if (requests != null) {
            for (CreateProgramParticipationRequest request : requests) {
                OrganizationEntity organization = findOrganization(request.organizationId());
                assertPortfolioOrganization(organization, "Participant organization");
                assertOrganizationIsActive(organization, "Participant organization");
                assertOrganizationsBelongToSameCustomer(ownerOrganization, organization);
                if (!includedOrganizations.add(organization.getId())) {
                    throw new IllegalArgumentException("Program participants cannot repeat the same organization.");
                }

                program.addParticipant(ProgramParticipationEntity.create(
                        actor,
                        program,
                        organization,
                        request.role(),
                        defaultValue(request.status(), ParticipationStatus.ACTIVE)), actor);
            }
        }

        if (!includedOrganizations.contains(ownerOrganization.getId())) {
            program.addParticipant(ProgramParticipationEntity.create(
                    actor,
                    program,
                    ownerOrganization,
                    ParticipationRole.INTERNAL,
                    ParticipationStatus.ACTIVE), actor);
        }
    }

    private ProjectEntity buildProject(ProgramEntity program, CreateProjectRequest request, String actor) {
        validateDateRange(request.plannedStartDate(), request.plannedEndDate(), "Project");
        if (projectRepository.existsByCodeIgnoreCase(request.code().trim())) {
            throw new IllegalArgumentException("Project code already exists.");
        }

        MilestoneTemplateEntity template = null;
        if (request.milestoneTemplateId() != null && !request.milestoneTemplateId().isBlank()) {
            template = findMilestoneTemplate(request.milestoneTemplateId());
        }

        ProjectEntity project = ProjectEntity.create(
                actor,
                program,
                request.name().trim(),
                request.code().trim().toUpperCase(),
                trimToNull(request.description()),
                defaultValue(request.status(), ProjectStatus.DRAFT),
                request.plannedStartDate(),
                request.plannedEndDate(),
                template != null ? template.getId() : null);

        if (template != null) {
            template.getItems().stream()
                    .sorted(java.util.Comparator.comparing(MilestoneTemplateItemEntity::getSortOrder))
                    .forEach(templateItem -> project.addMilestone(ProjectMilestoneEntity.create(
                            actor,
                            project,
                            templateItem.getId(),
                            templateItem.getName(),
                            templateItem.getSortOrder(),
                            ProjectMilestoneStatus.PLANNED,
                            request.plannedStartDate().plusWeeks(defaultValue(templateItem.getOffsetWeeks(), 0))), actor));
        }

        return project;
    }

    private OrganizationEntity findOrganization(String organizationId) {
        return organizationRepository.findById(organizationId)
                .orElseThrow(() -> new PortfolioNotFoundException("Organization", organizationId));
    }

    private ProgramEntity findProgram(String programId) {
        return programRepository.findById(programId)
                .orElseThrow(() -> new PortfolioNotFoundException("Program", programId));
    }

    private ProjectEntity findProject(String projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new PortfolioNotFoundException("Project", projectId));
    }

    private ProductEntity findProduct(String productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new PortfolioNotFoundException("Product", productId));
    }

    private ItemEntity findItem(String itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new PortfolioNotFoundException("Item", itemId));
    }

    private DeliverableEntity findDeliverable(String deliverableId) {
        return deliverableRepository.findById(deliverableId)
                .orElseThrow(() -> new PortfolioNotFoundException("Deliverable", deliverableId));
    }

    private DeliverableDocumentEntity findDeliverableDocument(String deliverableId, String documentId) {
        DeliverableDocumentEntity document = deliverableDocumentRepository.findById(documentId)
                .orElseThrow(() -> new PortfolioNotFoundException("Deliverable document", documentId));
        if (!document.getDeliverable().getId().equals(deliverableId)) {
            throw new IllegalArgumentException("Document does not belong to the informed deliverable.");
        }
        return document;
    }

    private MilestoneTemplateEntity findMilestoneTemplate(String milestoneTemplateId) {
        return milestoneTemplateRepository.findById(milestoneTemplateId)
                .orElseThrow(() -> new PortfolioNotFoundException("Milestone template", milestoneTemplateId));
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate, String resourceName) {
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException(resourceName + " end date cannot be before start date.");
        }
    }

    private void assertDocumentDeliverable(DeliverableEntity deliverable) {
        if (deliverable.getType() != DeliverableType.DOCUMENT) {
            throw new IllegalArgumentException("Document upload is only allowed for DOCUMENT deliverables.");
        }
    }

    private String resolveBucketName() {
        if (documentProperties.bucketName() == null || documentProperties.bucketName().isBlank()) {
            throw new IllegalStateException("Document storage bucket is not configured.");
        }
        return documentProperties.bucketName().trim();
    }

    private String buildStorageKey(DeliverableEntity deliverable, String fileName) {
        ItemEntity item = deliverable.getItem();
        ProductEntity product = item.getProduct();
        ProjectEntity project = product.getProject();
        ProgramEntity program = project.getProgram();
        String sanitizedFileName = sanitizeFileName(fileName);
        String keyPrefix = (documentProperties.keyPrefix() == null || documentProperties.keyPrefix().isBlank())
                ? "portfolio"
                : documentProperties.keyPrefix().trim();
        return "%s/organization/%s/program/%s/project/%s/deliverable/%s/document/%s/%s".formatted(
                keyPrefix,
                program.getOwnerOrganization().getId(),
                program.getId(),
                project.getId(),
                deliverable.getId(),
                PortfolioIds.newId("BIN"),
                sanitizedFileName);
    }

    private String sanitizeFileName(String fileName) {
        return fileName.trim().replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeName(String value) {
        return value.trim();
    }

    private String normalizeCode(String value) {
        return value.trim().toUpperCase();
    }

    private boolean matchesSearch(OrganizationResponse organization, String search) {
        if (search == null || search.isBlank()) {
            return true;
        }

        String normalizedSearch = search.trim().toLowerCase(Locale.ROOT);
        return organization.name().toLowerCase(Locale.ROOT).contains(normalizedSearch)
                || organization.code().toLowerCase(Locale.ROOT).contains(normalizedSearch)
                || organization.id().toLowerCase(Locale.ROOT).contains(normalizedSearch);
    }

    private <T> T defaultValue(T value, T fallback) {
        return value != null ? value : fallback;
    }

    private List<OrganizationEntity> visibleOrganizations(AuthenticatedUser actor) {
        if (canViewAllOrganizations(actor)) {
            return organizationRepository.findAllByOrderByNameAsc().stream()
                    .filter(this::isPortfolioOrganization)
                    .toList();
        }

        Set<String> visibleOrganizationIds = visibleOrganizationIds(actor);
        return organizationRepository.findAllByOrderByNameAsc().stream()
                .filter(this::isPortfolioOrganization)
                .filter(organization -> visibleOrganizationIds.contains(organization.getId()))
                .toList();
    }

    private List<ProgramEntity> visiblePrograms(AuthenticatedUser actor) {
        if (canViewAllOrganizations(actor)) {
            return programRepository.findAllByOrderByCreatedAtAsc();
        }

        Set<String> visibleOrganizationIds = visibleOrganizationIds(actor);
        return programRepository.findAllByOrderByCreatedAtAsc().stream()
                .filter(program -> visibleOrganizationIds.contains(program.getOwnerOrganization().getId()))
                .toList();
    }

    private Set<String> visibleOrganizationIds(AuthenticatedUser actor) {
        if (actor == null || actor.tenantId() == null || actor.tenantId().isBlank()) {
            return Set.of();
        }

        if (canViewAllOrganizations(actor)) {
            return organizationRepository.findAllByOrderByNameAsc().stream()
                    .filter(this::isPortfolioOrganization)
                    .map(OrganizationEntity::getId)
                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        }

        if (actor.tenantType() == TenantType.EXTERNAL && actor.hasRole(Role.SUPPORT)) {
            return Set.of(actor.tenantId());
        }

        return organizationDirectoryService.collectSubtreeIds(actor.tenantId());
    }

    private boolean canViewAllOrganizations(AuthenticatedUser actor) {
        return actor != null
                && actor.tenantType() == TenantType.INTERNAL
                && (actor.hasRole(Role.ADMIN) || actor.hasRole(Role.SUPPORT));
    }

    private boolean isPortfolioOrganization(OrganizationEntity organization) {
        return organization.getTenantType() == TenantType.EXTERNAL;
    }

    private void assertPortfolioOrganization(OrganizationEntity organization, String label) {
        if (!isPortfolioOrganization(organization)) {
            throw new IllegalArgumentException(label + " must be EXTERNAL.");
        }
    }

    private void assertCanViewOrganization(AuthenticatedUser actor, OrganizationEntity organization) {
        if (!visibleOrganizationIds(actor).contains(organization.getId())) {
            throw new AccessDeniedException("Organization is outside the visible hierarchy for the authenticated user.");
        }
    }

    private void assertCanAccessOrganization(AuthenticatedUser actor, OrganizationEntity organization, Action action) {
        AuthorizationContext context = AuthorizationContext.builder(
                        com.oryzem.programmanagementsystem.authorization.AppModule.TENANT,
                        action)
                .resourceTenantId(organization.getId())
                .resourceTenantType(organization.getTenantType())
                .build();
        assertAllowed(authorizationService.decide(actor, context));
    }

    private void assertCanViewProgram(AuthenticatedUser actor, ProgramEntity program) {
        assertCanViewPortfolio(actor, program.getOwnerOrganization().getId());
    }

    private void assertCanViewPortfolio(AuthenticatedUser actor, String organizationId) {
        AuthorizationContext.Builder contextBuilder = AuthorizationContext.builder(AppModule.PORTFOLIO, Action.VIEW);
        if (organizationId != null && !organizationId.isBlank()) {
            contextBuilder.resourceTenantId(organizationId).resourceTenantType(TenantType.EXTERNAL);
        }
        assertAllowed(authorizationService.decide(actor, contextBuilder.build()));
        assertCanOperateOnVisiblePortfolio(actor, organizationId);
    }

    private void assertCanManageProgram(AuthenticatedUser actor, String organizationId) {
        assertCanMutatePortfolio(actor, organizationId, Action.CREATE);
        assertPortfolioRoles(actor, "Only ADMIN can manage programs.", Role.ADMIN);
    }

    private void assertCanManageProjectLayer(AuthenticatedUser actor, String organizationId) {
        assertCanMutatePortfolio(actor, organizationId, Action.CREATE);
        assertPortfolioRoles(actor, "Only ADMIN or MANAGER can manage projects, products and open issues.", Role.ADMIN, Role.MANAGER);
    }

    private void assertCanManageTaskLayer(AuthenticatedUser actor, String organizationId) {
        assertCanMutatePortfolio(actor, organizationId, Action.CREATE);
        assertPortfolioRoles(actor, "Only ADMIN, MANAGER or MEMBER can manage execution items, deliverables and documents.",
                Role.ADMIN,
                Role.MANAGER,
                Role.MEMBER);
    }

    private void assertCanConfigurePortfolio(AuthenticatedUser actor) {
        assertAllowed(authorizationService.decide(
                actor,
                AuthorizationContext.builder(AppModule.PORTFOLIO, Action.CONFIGURE).build()));
        assertPortfolioRoles(actor, "Only ADMIN can manage milestone templates.", Role.ADMIN);
    }

    private void assertCanMutatePortfolio(AuthenticatedUser actor, String organizationId, Action action) {
        AuthorizationContext.Builder contextBuilder = AuthorizationContext.builder(AppModule.PORTFOLIO, action);
        if (organizationId != null && !organizationId.isBlank()) {
            contextBuilder.resourceTenantId(organizationId).resourceTenantType(TenantType.EXTERNAL);
        }
        assertAllowed(authorizationService.decide(actor, contextBuilder.build()));
        assertCanOperateOnVisiblePortfolio(actor, organizationId);
    }

    private void assertCanOperateOnVisiblePortfolio(AuthenticatedUser actor, String organizationId) {
        if (organizationId == null || organizationId.isBlank()) {
            return;
        }

        if (!visibleOrganizationIds(actor).contains(organizationId)) {
            throw new AccessDeniedException("Portfolio is outside the visible hierarchy for the authenticated user.");
        }
    }

    private void assertPortfolioRoles(AuthenticatedUser actor, String message, Role... allowedRoles) {
        for (Role allowedRole : allowedRoles) {
            if (actor.hasRole(allowedRole)) {
                return;
            }
        }
        throw new AccessDeniedException(message);
    }

    private void assertCanCreateRootCustomer(AuthenticatedUser actor) {
        if (actor == null || !actor.hasRole(Role.ADMIN) || actor.tenantType() != TenantType.INTERNAL) {
            throw new AccessDeniedException("Only INTERNAL admins can create root customer organizations.");
        }
    }

    private void assertCanCreateChildOrganization(AuthenticatedUser actor, OrganizationEntity parentOrganization) {
        if (actor == null || !actor.hasRole(Role.ADMIN)) {
            throw new AccessDeniedException("Only admins can create child organizations.");
        }

        if (actor.tenantType() == TenantType.INTERNAL) {
            return;
        }

        if (actor.tenantType() == TenantType.EXTERNAL
                && visibleOrganizationIds(actor).contains(parentOrganization.getId())) {
            return;
        }

        throw new AccessDeniedException("Organization is outside the manageable hierarchy for the authenticated user.");
    }

    private void assertCanManageOrganization(AuthenticatedUser actor, OrganizationEntity organization) {
        if (actor == null || !actor.hasRole(Role.ADMIN)) {
            throw new AccessDeniedException("Only admins can manage organizations.");
        }

        if (actor.tenantType() == TenantType.INTERNAL) {
            return;
        }

        if (actor.tenantType() == TenantType.EXTERNAL
                && visibleOrganizationIds(actor).contains(organization.getId())) {
            return;
        }

        throw new AccessDeniedException("Organization is outside the manageable hierarchy for the authenticated user.");
    }

    private void assertOrganizationCanOwnChildren(OrganizationEntity parentOrganization) {
        if (parentOrganization.getTenantType() != TenantType.EXTERNAL) {
            throw new IllegalArgumentException("Only EXTERNAL organizations can own child organizations.");
        }
        assertOrganizationIsActive(parentOrganization, "Parent organization");
    }

    private OrganizationEntity findPortfolioOrganization(String organizationId) {
        OrganizationEntity organization = findOrganization(organizationId);
        if (!isPortfolioOrganization(organization)) {
            throw new PortfolioNotFoundException("Organization", organizationId);
        }
        return organization;
    }

    private void assertOrganizationsBelongToSameCustomer(
            OrganizationEntity ownerOrganization,
            OrganizationEntity participantOrganization) {
        String ownerCustomerId = ownerOrganization.getCustomerOrganization() != null
                ? ownerOrganization.getCustomerOrganization().getId()
                : null;
        String participantCustomerId = participantOrganization.getCustomerOrganization() != null
                ? participantOrganization.getCustomerOrganization().getId()
                : null;
        if (ownerCustomerId == null || !ownerCustomerId.equals(participantCustomerId)) {
            throw new IllegalArgumentException("Program organizations must belong to the same customer hierarchy.");
        }
    }

    private void ensureOrganizationIsMutable(OrganizationEntity organization) {
        if (organization.getStatus() == OrganizationStatus.INACTIVE) {
            throw new IllegalArgumentException("Inactive organizations cannot be updated.");
        }
    }

    private void assertOrganizationIsActive(OrganizationEntity organization, String label) {
        if (organization.getStatus() != OrganizationStatus.ACTIVE) {
            throw new IllegalArgumentException(label + " is inactive.");
        }
    }

    private void assertOrganizationSetupComplete(String organizationId, String label) {
        if (resolveOrganizationSetupStatus(organizationId) == OrganizationSetupStatus.INCOMPLETED) {
            throw new IllegalArgumentException(label + " is incomplete and requires an invited or active ADMIN user.");
        }
    }

    private void ensureOrganizationHasNoInvitedOrActiveUsers(String organizationId) {
        boolean hasActiveUsers = userRepository.findByTenantId(organizationId).stream()
                .anyMatch(user -> user.status() != UserStatus.INACTIVE);
        if (hasActiveUsers) {
            throw new IllegalArgumentException(
                    "Organization can only be inactivated after all invited or active users are inactivated.");
        }
    }

    private void ensureOrganizationHasNoActiveChildren(String organizationId) {
        boolean hasActiveChildren = organizationRepository.findAllByParentOrganizationIdOrderByNameAsc(organizationId).stream()
                .anyMatch(child -> child.getStatus() == OrganizationStatus.ACTIVE);
        if (hasActiveChildren) {
            throw new IllegalArgumentException("Organization can only be inactivated after all active child organizations are inactivated.");
        }
    }

    private void ensureOrganizationHasNoActiveProjects(String organizationId) {
        boolean hasActiveProjects = programRepository.findAllByOrderByCreatedAtAsc().stream()
                .filter(program -> program.getOwnerOrganization().getId().equals(organizationId))
                .flatMap(program -> program.getProjects().stream())
                .anyMatch(project -> project.getStatus() == ProjectStatus.ACTIVE);
        if (hasActiveProjects) {
            throw new IllegalArgumentException("Organization can only be inactivated after all active projects are closed or inactivated.");
        }
    }

    private OrganizationResponse toOrganizationResponse(
            OrganizationEntity organization,
            OrganizationManagementSnapshot snapshot) {
        String organizationId = organization.getId();
        OrganizationSetupStatus setupStatus = snapshot.setupStatuses()
                .getOrDefault(organizationId, OrganizationSetupStatus.INCOMPLETED);
        OrganizationUserSummaryResponse userSummary = snapshot.userSummaries()
                .getOrDefault(organizationId, OrganizationUserSummaryResponse.empty());
        OrganizationProgramSummaryResponse programSummary = snapshot.programSummaries()
                .getOrDefault(organizationId, OrganizationProgramSummaryResponse.empty());
        boolean hasActiveChildren = organizationRepository.findAllByParentOrganizationIdOrderByNameAsc(organizationId).stream()
                .anyMatch(child -> child.getStatus() == OrganizationStatus.ACTIVE);
        boolean hasActiveProjects = programRepository.findAllByOrderByCreatedAtAsc().stream()
                .filter(program -> program.getOwnerOrganization().getId().equals(organizationId))
                .flatMap(program -> program.getProjects().stream())
                .anyMatch(project -> project.getStatus() == ProjectStatus.ACTIVE);

        boolean canInactivate = organization.getStatus() == OrganizationStatus.ACTIVE
                && userSummary.invitedCount() == 0
                && userSummary.activeCount() == 0
                && !hasActiveChildren
                && !hasActiveProjects;
        String inactivationBlockedReason = null;
        if (organization.getStatus() == OrganizationStatus.INACTIVE) {
            inactivationBlockedReason = "Organization is already inactive.";
        } else if (userSummary.invitedCount() > 0 || userSummary.activeCount() > 0) {
            inactivationBlockedReason =
                    "Organization can only be inactivated after all invited or active users are inactivated.";
        } else if (hasActiveChildren) {
            inactivationBlockedReason =
                    "Organization can only be inactivated after all active child organizations are inactivated.";
        } else if (hasActiveProjects) {
            inactivationBlockedReason =
                    "Organization can only be inactivated after all active projects are closed or inactivated.";
        }

        long childrenCount = snapshot.childrenCountByOrganizationId()
                .getOrDefault(organizationId, organizationDirectoryService.countDirectChildren(organizationId));

        return OrganizationResponse.from(
                organization,
                (int) childrenCount,
                setupStatus,
                userSummary,
                programSummary,
                canInactivate,
                inactivationBlockedReason);
    }

    private OrganizationResponse toOrganizationResponse(OrganizationEntity organization) {
        return toOrganizationResponse(
                organization,
                buildOrganizationManagementSnapshot(List.of(organization)));
    }

    private void assertAllowed(AuthorizationDecision decision) {
        if (!decision.allowed()) {
            throw new AccessDeniedException(decision.reason());
        }
    }

    private OrganizationManagementSnapshot buildOrganizationManagementSnapshot(List<OrganizationEntity> organizations) {
        Set<String> organizationIds = organizations.stream()
                .map(OrganizationEntity::getId)
                .collect(java.util.stream.Collectors.toSet());

        Map<String, UserCounters> userCountersByOrganization = new HashMap<>();
        for (ManagedUser user : userRepository.findAll()) {
            if (!organizationIds.contains(user.tenantId())) {
                continue;
            }

            userCountersByOrganization
                    .computeIfAbsent(user.tenantId(), ignored -> new UserCounters())
                    .add(user);
        }

        Map<String, ProgramCounters> programCountersByOrganization = new HashMap<>();
        for (ProgramEntity program : programRepository.findAllByOrderByCreatedAtAsc()) {
            String ownerOrganizationId = program.getOwnerOrganization().getId();
            if (organizationIds.contains(ownerOrganizationId)) {
                programCountersByOrganization
                        .computeIfAbsent(ownerOrganizationId, ignored -> new ProgramCounters())
                        .incrementOwned();
            }

            program.getParticipants().stream()
                    .map(participation -> participation.getOrganization().getId())
                    .filter(organizationIds::contains)
                    .filter(participantOrganizationId -> !participantOrganizationId.equals(ownerOrganizationId))
                    .distinct()
                    .forEach(participantOrganizationId -> programCountersByOrganization
                            .computeIfAbsent(participantOrganizationId, ignored -> new ProgramCounters())
                            .incrementParticipating());
        }

        Map<String, OrganizationSetupStatus> setupStatuses = new HashMap<>();
        Map<String, OrganizationUserSummaryResponse> userSummaries = new HashMap<>();
        Map<String, OrganizationProgramSummaryResponse> programSummaries = new HashMap<>();
        Map<String, Long> childrenCountByOrganizationId = organizations.stream()
                .filter(organization -> organization.getParentOrganization() != null)
                .collect(java.util.stream.Collectors.groupingBy(
                        organization -> organization.getParentOrganization().getId(),
                        java.util.stream.Collectors.counting()));
        for (String organizationId : organizationIds) {
            UserCounters userCounters = userCountersByOrganization.getOrDefault(organizationId, new UserCounters());
            ProgramCounters programCounters = programCountersByOrganization.getOrDefault(
                    organizationId,
                    new ProgramCounters());

            setupStatuses.put(
                    organizationId,
                    userCounters.hasInvitedOrActiveAdmin()
                            ? OrganizationSetupStatus.COMPLETED
                            : OrganizationSetupStatus.INCOMPLETED);
            userSummaries.put(organizationId, userCounters.toResponse());
            programSummaries.put(organizationId, programCounters.toResponse());
        }

        return new OrganizationManagementSnapshot(setupStatuses, userSummaries, programSummaries, childrenCountByOrganizationId);
    }

    private OrganizationSetupStatus resolveOrganizationSetupStatus(String organizationId) {
        return userRepository.hasInvitedOrActiveAdmin(organizationId)
                ? OrganizationSetupStatus.COMPLETED
                : OrganizationSetupStatus.INCOMPLETED;
    }

    private record OrganizationManagementSnapshot(
            Map<String, OrganizationSetupStatus> setupStatuses,
            Map<String, OrganizationUserSummaryResponse> userSummaries,
            Map<String, OrganizationProgramSummaryResponse> programSummaries,
            Map<String, Long> childrenCountByOrganizationId) {
    }

    private static final class UserCounters {

        private int invitedCount;
        private int activeCount;
        private int inactiveCount;
        private boolean hasInvitedOrActiveAdmin;

        void add(ManagedUser user) {
            switch (user.status()) {
                case INVITED -> invitedCount++;
                case ACTIVE -> activeCount++;
                case INACTIVE -> inactiveCount++;
            }

            if (user.role() == Role.ADMIN && user.status() != UserStatus.INACTIVE) {
                hasInvitedOrActiveAdmin = true;
            }
        }

        boolean hasInvitedOrActiveAdmin() {
            return hasInvitedOrActiveAdmin;
        }

        OrganizationUserSummaryResponse toResponse() {
            return new OrganizationUserSummaryResponse(
                    invitedCount,
                    activeCount,
                    inactiveCount,
                    invitedCount + activeCount + inactiveCount);
        }
    }

    private static final class ProgramCounters {

        private int ownedCount;
        private int participatingCount;

        void incrementOwned() {
            ownedCount++;
        }

        void incrementParticipating() {
            participatingCount++;
        }

        OrganizationProgramSummaryResponse toResponse() {
            return new OrganizationProgramSummaryResponse(
                    ownedCount,
                    participatingCount,
                    ownedCount + participatingCount);
        }
    }
}
