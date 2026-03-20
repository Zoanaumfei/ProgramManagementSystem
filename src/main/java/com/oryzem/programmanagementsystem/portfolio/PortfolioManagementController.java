package com.oryzem.programmanagementsystem.portfolio;

import com.oryzem.programmanagementsystem.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.authorization.AuthenticatedUserMapper;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;

@RestController
@RequestMapping("/api/portfolio")
public class PortfolioManagementController {

    private final PortfolioManagementService portfolioManagementService;
    private final AuthenticatedUserMapper authenticatedUserMapper;

    public PortfolioManagementController(
            PortfolioManagementService portfolioManagementService,
            AuthenticatedUserMapper authenticatedUserMapper) {
        this.portfolioManagementService = portfolioManagementService;
        this.authenticatedUserMapper = authenticatedUserMapper;
    }

    @GetMapping("/organizations")
    public List<OrganizationResponse> listOrganizations(
            Authentication authentication,
            @RequestParam(required = false) OrganizationStatus status,
            @RequestParam(required = false) OrganizationSetupStatus setupStatus,
            @RequestParam(required = false) String customerOrganizationId,
            @RequestParam(required = false) String parentOrganizationId,
            @RequestParam(required = false) Integer hierarchyLevel,
            @RequestParam(required = false) String search) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        return portfolioManagementService.listOrganizations(
                actor,
                status,
                setupStatus,
                customerOrganizationId,
                parentOrganizationId,
                hierarchyLevel,
                search);
    }

    @GetMapping("/organizations/{organizationId}")
    public OrganizationResponse getOrganization(
            Authentication authentication,
            @PathVariable String organizationId) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        return portfolioManagementService.getOrganization(organizationId, actor);
    }

    @PostMapping("/organizations")
    public ResponseEntity<OrganizationResponse> createOrganization(
            Authentication authentication,
            @Valid @RequestBody CreateOrganizationRequest request) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        return ResponseEntity.ok(portfolioManagementService.createOrganization(request, actor));
    }

    @PutMapping("/organizations/{organizationId}")
    public ResponseEntity<OrganizationResponse> updateOrganization(
            Authentication authentication,
            @PathVariable String organizationId,
            @Valid @RequestBody UpdateOrganizationRequest request) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        return ResponseEntity.ok(portfolioManagementService.updateOrganization(organizationId, request, actor));
    }

    @DeleteMapping("/organizations/{organizationId}")
    public ResponseEntity<OrganizationResponse> inactivateOrganization(
            Authentication authentication,
            @PathVariable String organizationId) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        return ResponseEntity.ok(portfolioManagementService.inactivateOrganization(organizationId, actor));
    }

    @GetMapping("/milestone-templates")
    public List<MilestoneTemplateResponse> listMilestoneTemplates(Authentication authentication) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        return portfolioManagementService.listMilestoneTemplates(actor);
    }

    @PostMapping("/milestone-templates")
    public ResponseEntity<MilestoneTemplateResponse> createMilestoneTemplate(
            Authentication authentication,
            @Valid @RequestBody CreateMilestoneTemplateRequest request) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        return ResponseEntity.ok(portfolioManagementService.createMilestoneTemplate(request, actor));
    }

    @GetMapping("/programs")
    public List<ProgramSummaryResponse> listPrograms(
            Authentication authentication,
            @RequestParam(required = false) String ownerOrganizationId) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        return portfolioManagementService.listPrograms(actor, ownerOrganizationId);
    }

    @GetMapping("/programs/{programId}")
    public ProgramDetailResponse getProgram(
            Authentication authentication,
            @PathVariable String programId) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        return portfolioManagementService.getProgram(programId, actor);
    }

    @PostMapping("/programs")
    public ResponseEntity<ProgramDetailResponse> createProgram(
            Authentication authentication,
            @Valid @RequestBody CreateProgramRequest request) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        return ResponseEntity.ok(portfolioManagementService.createProgram(request, actor));
    }

    @PostMapping("/programs/{programId}/projects")
    public ResponseEntity<ProjectResponse> createProject(
            Authentication authentication,
            @PathVariable String programId,
            @Valid @RequestBody CreateProjectRequest request) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        return ResponseEntity.ok(portfolioManagementService.createProject(programId, request, actor));
    }

    @PostMapping("/projects/{projectId}/products")
    public ResponseEntity<ProductResponse> createProduct(
            Authentication authentication,
            @PathVariable String projectId,
            @Valid @RequestBody CreateProductRequest request) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        return ResponseEntity.ok(portfolioManagementService.createProduct(projectId, request, actor));
    }

    @PostMapping("/products/{productId}/items")
    public ResponseEntity<ItemResponse> createItem(
            Authentication authentication,
            @PathVariable String productId,
            @Valid @RequestBody CreateItemRequest request) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        return ResponseEntity.ok(portfolioManagementService.createItem(productId, request, actor));
    }

    @PostMapping("/items/{itemId}/deliverables")
    public ResponseEntity<DeliverableResponse> createDeliverable(
            Authentication authentication,
            @PathVariable String itemId,
            @Valid @RequestBody CreateDeliverableRequest request) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        return ResponseEntity.ok(portfolioManagementService.createDeliverable(itemId, request, actor));
    }

    @GetMapping("/deliverables/{deliverableId}/documents")
    public List<DeliverableDocumentResponse> listDeliverableDocuments(
            Authentication authentication,
            @PathVariable String deliverableId) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        return portfolioManagementService.listDeliverableDocuments(deliverableId, actor);
    }

    @PostMapping("/deliverables/{deliverableId}/documents/upload-url")
    public ResponseEntity<DeliverableDocumentUploadResponse> prepareDeliverableDocumentUpload(
            Authentication authentication,
            @PathVariable String deliverableId,
            @Valid @RequestBody PrepareDeliverableDocumentUploadRequest request) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        return ResponseEntity.ok(portfolioManagementService.prepareDeliverableDocumentUpload(
                deliverableId,
                request,
                actor));
    }

    @PostMapping("/deliverables/{deliverableId}/documents/{documentId}/complete")
    public ResponseEntity<DeliverableDocumentResponse> completeDeliverableDocumentUpload(
            Authentication authentication,
            @PathVariable String deliverableId,
            @PathVariable String documentId) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        return ResponseEntity.ok(portfolioManagementService.completeDeliverableDocumentUpload(
                deliverableId,
                documentId,
                actor));
    }

    @PostMapping("/deliverables/{deliverableId}/documents/{documentId}/download-url")
    public ResponseEntity<DeliverableDocumentDownloadResponse> createDeliverableDocumentDownload(
            Authentication authentication,
            @PathVariable String deliverableId,
            @PathVariable String documentId) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        return ResponseEntity.ok(portfolioManagementService.createDeliverableDocumentDownload(deliverableId, documentId, actor));
    }

    @DeleteMapping("/deliverables/{deliverableId}/documents/{documentId}")
    public ResponseEntity<DeliverableDocumentResponse> deleteDeliverableDocument(
            Authentication authentication,
            @PathVariable String deliverableId,
            @PathVariable String documentId) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        return ResponseEntity.ok(portfolioManagementService.deleteDeliverableDocument(
                deliverableId,
                documentId,
                actor));
    }

    @PostMapping("/programs/{programId}/open-issues")
    public ResponseEntity<OpenIssueResponse> createOpenIssue(
            Authentication authentication,
            @PathVariable String programId,
            @Valid @RequestBody CreateOpenIssueRequest request) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        return ResponseEntity.ok(portfolioManagementService.createOpenIssue(programId, request, actor));
    }
}
