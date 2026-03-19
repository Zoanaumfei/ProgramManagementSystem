package com.oryzem.programmanagementsystem.portfolio;

import com.oryzem.programmanagementsystem.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.authorization.TenantType;
import com.oryzem.programmanagementsystem.users.UserRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.List;
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
        this.documentStorageGateway = documentStorageGateway;
        this.documentProperties = documentProperties;
    }

    @Transactional(readOnly = true)
    public List<OrganizationResponse> listOrganizations() {
        return organizationRepository.findAllByOrderByNameAsc().stream()
                .map(this::toOrganizationResponse)
                .toList();
    }

    public OrganizationResponse createOrganization(CreateOrganizationRequest request, AuthenticatedUser actor) {
        assertCanCreateOrganization(actor);
        if (organizationRepository.existsByCodeIgnoreCase(request.code().trim())) {
            throw new IllegalArgumentException("Organization code already exists.");
        }

        OrganizationEntity organization = OrganizationEntity.create(
                actor.username(),
                request.name().trim(),
                request.code().trim().toUpperCase(),
                defaultValue(request.status(), OrganizationStatus.ACTIVE));
        return toOrganizationResponse(organizationRepository.save(organization));
    }

    @Transactional(readOnly = true)
    public List<MilestoneTemplateResponse> listMilestoneTemplates() {
        return milestoneTemplateRepository.findAllByOrderByNameAsc().stream()
                .map(MilestoneTemplateResponse::from)
                .toList();
    }

    public MilestoneTemplateResponse createMilestoneTemplate(CreateMilestoneTemplateRequest request, String actor) {
        MilestoneTemplateEntity template = MilestoneTemplateEntity.create(
                actor,
                request.name().trim(),
                trimToNull(request.description()),
                defaultValue(request.status(), MilestoneTemplateStatus.ACTIVE));

        request.items().stream()
                .sorted(java.util.Comparator.comparing(CreateMilestoneTemplateItemRequest::sortOrder))
                .forEach(itemRequest -> template.addItem(MilestoneTemplateItemEntity.create(
                        actor,
                        template,
                        itemRequest.name().trim(),
                        itemRequest.sortOrder(),
                        itemRequest.required(),
                        itemRequest.offsetWeeks()), actor));

        return MilestoneTemplateResponse.from(milestoneTemplateRepository.save(template));
    }

    @Transactional(readOnly = true)
    public List<ProgramSummaryResponse> listPrograms() {
        return programRepository.findAllByOrderByCreatedAtAsc().stream()
                .map(ProgramSummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProgramDetailResponse getProgram(String programId) {
        return ProgramDetailResponse.from(findProgram(programId));
    }

    public ProgramDetailResponse createProgram(CreateProgramRequest request, String actor) {
        validateDateRange(request.plannedStartDate(), request.plannedEndDate(), "Program");
        if (programRepository.existsByCodeIgnoreCase(request.code().trim())) {
            throw new IllegalArgumentException("Program code already exists.");
        }
        if (request.initialProject() == null) {
            throw new IllegalArgumentException("Program requires an initial project.");
        }

        OrganizationEntity ownerOrganization = findOrganization(request.ownerOrganizationId());
        assertOrganizationSetupComplete(ownerOrganization.getId(), "Owner organization");
        ProgramEntity program = ProgramEntity.create(
                actor,
                request.name().trim(),
                request.code().trim().toUpperCase(),
                trimToNull(request.description()),
                ProgramStatus.DRAFT,
                request.plannedStartDate(),
                request.plannedEndDate(),
                ownerOrganization);

        addParticipants(program, ownerOrganization, request.participants(), actor);
        ProjectEntity initialProject = buildProject(program, request.initialProject(), actor);
        program.addProject(initialProject, actor);

        return ProgramDetailResponse.from(programRepository.save(program));
    }

    public ProjectResponse createProject(String programId, CreateProjectRequest request, String actor) {
        ProgramEntity program = findProgram(programId);
        ProjectEntity project = buildProject(program, request, actor);
        program.addProject(project, actor);
        programRepository.save(program);
        return ProjectResponse.from(project);
    }

    public ProductResponse createProduct(String projectId, CreateProductRequest request, String actor) {
        if (productRepository.existsByCodeIgnoreCase(request.code().trim())) {
            throw new IllegalArgumentException("Product code already exists.");
        }

        ProjectEntity project = findProject(projectId);
        ProductEntity product = ProductEntity.create(
                actor,
                project,
                request.name().trim(),
                request.code().trim().toUpperCase(),
                trimToNull(request.description()),
                defaultValue(request.status(), ProductStatus.ACTIVE));
        project.addProduct(product, actor);
        projectRepository.save(project);
        return ProductResponse.from(product);
    }

    public ItemResponse createItem(String productId, CreateItemRequest request, String actor) {
        if (itemRepository.existsByCodeIgnoreCase(request.code().trim())) {
            throw new IllegalArgumentException("Item code already exists.");
        }

        ProductEntity product = findProduct(productId);
        ItemEntity item = ItemEntity.create(
                actor,
                product,
                request.name().trim(),
                request.code().trim().toUpperCase(),
                trimToNull(request.description()),
                defaultValue(request.status(), ItemStatus.ACTIVE));
        product.addItem(item, actor);
        productRepository.save(product);
        return ItemResponse.from(item);
    }

    public DeliverableResponse createDeliverable(String itemId, CreateDeliverableRequest request, String actor) {
        validateDateRange(request.plannedDate(), request.dueDate(), "Deliverable");

        ItemEntity item = findItem(itemId);
        DeliverableEntity deliverable = DeliverableEntity.create(
                actor,
                item,
                request.name().trim(),
                trimToNull(request.description()),
                request.type(),
                defaultValue(request.status(), DeliverableStatus.PENDING),
                request.plannedDate(),
                request.dueDate());
        item.addDeliverable(deliverable, actor);
        itemRepository.save(item);
        return DeliverableResponse.from(deliverable);
    }

    public OpenIssueResponse createOpenIssue(String programId, CreateOpenIssueRequest request, String actor) {
        ProgramEntity program = findProgram(programId);
        OpenIssueEntity issue = OpenIssueEntity.create(
                actor,
                program,
                request.title().trim(),
                trimToNull(request.description()),
                defaultValue(request.status(), OpenIssueStatus.OPEN),
                request.severity(),
                request.openedAt() != null ? request.openedAt() : OffsetDateTime.now());
        program.addOpenIssue(issue, actor);
        programRepository.save(program);
        return OpenIssueResponse.from(issue);
    }

    @Transactional(readOnly = true)
    public List<DeliverableDocumentResponse> listDeliverableDocuments(String deliverableId) {
        findDeliverable(deliverableId);
        return deliverableDocumentRepository.findByDeliverableIdOrderByCreatedAtAsc(deliverableId).stream()
                .map(DeliverableDocumentResponse::from)
                .toList();
    }

    public DeliverableDocumentUploadResponse prepareDeliverableDocumentUpload(
            String deliverableId,
            PrepareDeliverableDocumentUploadRequest request,
            String actor) {
        DeliverableEntity deliverable = findDeliverable(deliverableId);
        assertDocumentDeliverable(deliverable);

        DeliverableDocumentEntity document = DeliverableDocumentEntity.createPendingUpload(
                actor,
                deliverable,
                request.fileName().trim(),
                request.contentType().trim(),
                request.fileSize(),
                resolveBucketName(),
                buildStorageKey(deliverable, request.fileName()));
        deliverable.addDocument(document, actor);
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
            String actor) {
        DeliverableDocumentEntity document = findDeliverableDocument(deliverableId, documentId);
        if (document.getStatus() == DeliverableDocumentStatus.DELETED) {
            throw new IllegalArgumentException("Deleted document cannot be completed.");
        }

        documentStorageGateway.assertObjectExists(document);
        document.markAvailable(actor);
        return DeliverableDocumentResponse.from(deliverableDocumentRepository.save(document));
    }

    public DeliverableDocumentDownloadResponse createDeliverableDocumentDownload(
            String deliverableId,
            String documentId) {
        DeliverableDocumentEntity document = findDeliverableDocument(deliverableId, documentId);
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
            String actor) {
        DeliverableDocumentEntity document = findDeliverableDocument(deliverableId, documentId);
        document.markDeleted(actor);
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

    private <T> T defaultValue(T value, T fallback) {
        return value != null ? value : fallback;
    }

    private void assertCanCreateOrganization(AuthenticatedUser actor) {
        if (actor == null || !actor.isAdmin() || actor.tenantType() != TenantType.INTERNAL) {
            throw new AccessDeniedException("Only INTERNAL admins can create organizations.");
        }
    }

    private void assertOrganizationSetupComplete(String organizationId, String label) {
        if (resolveOrganizationSetupStatus(organizationId) == OrganizationSetupStatus.INCOMPLETED) {
            throw new IllegalArgumentException(label + " is incomplete and requires an invited or active ADMIN user.");
        }
    }

    private OrganizationResponse toOrganizationResponse(OrganizationEntity organization) {
        return OrganizationResponse.from(organization, resolveOrganizationSetupStatus(organization.getId()));
    }

    private OrganizationSetupStatus resolveOrganizationSetupStatus(String organizationId) {
        return userRepository.hasInvitedOrActiveAdmin(organizationId)
                ? OrganizationSetupStatus.COMPLETED
                : OrganizationSetupStatus.INCOMPLETED;
    }
}
