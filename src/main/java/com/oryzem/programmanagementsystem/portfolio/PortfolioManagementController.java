package com.oryzem.programmanagementsystem.portfolio;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;

@RestController
@RequestMapping("/api/portfolio")
public class PortfolioManagementController {

    private final PortfolioManagementService portfolioManagementService;

    public PortfolioManagementController(PortfolioManagementService portfolioManagementService) {
        this.portfolioManagementService = portfolioManagementService;
    }

    @GetMapping("/organizations")
    public List<OrganizationResponse> listOrganizations() {
        return portfolioManagementService.listOrganizations();
    }

    @PostMapping("/organizations")
    public ResponseEntity<OrganizationResponse> createOrganization(
            Authentication authentication,
            @Valid @RequestBody CreateOrganizationRequest request) {
        return ResponseEntity.ok(portfolioManagementService.createOrganization(request, authentication.getName()));
    }

    @GetMapping("/milestone-templates")
    public List<MilestoneTemplateResponse> listMilestoneTemplates() {
        return portfolioManagementService.listMilestoneTemplates();
    }

    @PostMapping("/milestone-templates")
    public ResponseEntity<MilestoneTemplateResponse> createMilestoneTemplate(
            Authentication authentication,
            @Valid @RequestBody CreateMilestoneTemplateRequest request) {
        return ResponseEntity.ok(portfolioManagementService.createMilestoneTemplate(request, authentication.getName()));
    }

    @GetMapping("/programs")
    public List<ProgramSummaryResponse> listPrograms() {
        return portfolioManagementService.listPrograms();
    }

    @GetMapping("/programs/{programId}")
    public ProgramDetailResponse getProgram(@PathVariable String programId) {
        return portfolioManagementService.getProgram(programId);
    }

    @PostMapping("/programs")
    public ResponseEntity<ProgramDetailResponse> createProgram(
            Authentication authentication,
            @Valid @RequestBody CreateProgramRequest request) {
        return ResponseEntity.ok(portfolioManagementService.createProgram(request, authentication.getName()));
    }

    @PostMapping("/programs/{programId}/projects")
    public ResponseEntity<ProjectResponse> createProject(
            Authentication authentication,
            @PathVariable String programId,
            @Valid @RequestBody CreateProjectRequest request) {
        return ResponseEntity.ok(portfolioManagementService.createProject(programId, request, authentication.getName()));
    }

    @PostMapping("/projects/{projectId}/products")
    public ResponseEntity<ProductResponse> createProduct(
            Authentication authentication,
            @PathVariable String projectId,
            @Valid @RequestBody CreateProductRequest request) {
        return ResponseEntity.ok(portfolioManagementService.createProduct(projectId, request, authentication.getName()));
    }

    @PostMapping("/products/{productId}/items")
    public ResponseEntity<ItemResponse> createItem(
            Authentication authentication,
            @PathVariable String productId,
            @Valid @RequestBody CreateItemRequest request) {
        return ResponseEntity.ok(portfolioManagementService.createItem(productId, request, authentication.getName()));
    }

    @PostMapping("/items/{itemId}/deliverables")
    public ResponseEntity<DeliverableResponse> createDeliverable(
            Authentication authentication,
            @PathVariable String itemId,
            @Valid @RequestBody CreateDeliverableRequest request) {
        return ResponseEntity.ok(portfolioManagementService.createDeliverable(itemId, request, authentication.getName()));
    }

    @GetMapping("/deliverables/{deliverableId}/documents")
    public List<DeliverableDocumentResponse> listDeliverableDocuments(@PathVariable String deliverableId) {
        return portfolioManagementService.listDeliverableDocuments(deliverableId);
    }

    @PostMapping("/deliverables/{deliverableId}/documents/upload-url")
    public ResponseEntity<DeliverableDocumentUploadResponse> prepareDeliverableDocumentUpload(
            Authentication authentication,
            @PathVariable String deliverableId,
            @Valid @RequestBody PrepareDeliverableDocumentUploadRequest request) {
        return ResponseEntity.ok(portfolioManagementService.prepareDeliverableDocumentUpload(
                deliverableId,
                request,
                authentication.getName()));
    }

    @PostMapping("/deliverables/{deliverableId}/documents/{documentId}/complete")
    public ResponseEntity<DeliverableDocumentResponse> completeDeliverableDocumentUpload(
            Authentication authentication,
            @PathVariable String deliverableId,
            @PathVariable String documentId) {
        return ResponseEntity.ok(portfolioManagementService.completeDeliverableDocumentUpload(
                deliverableId,
                documentId,
                authentication.getName()));
    }

    @PostMapping("/deliverables/{deliverableId}/documents/{documentId}/download-url")
    public ResponseEntity<DeliverableDocumentDownloadResponse> createDeliverableDocumentDownload(
            @PathVariable String deliverableId,
            @PathVariable String documentId) {
        return ResponseEntity.ok(portfolioManagementService.createDeliverableDocumentDownload(deliverableId, documentId));
    }

    @DeleteMapping("/deliverables/{deliverableId}/documents/{documentId}")
    public ResponseEntity<DeliverableDocumentResponse> deleteDeliverableDocument(
            Authentication authentication,
            @PathVariable String deliverableId,
            @PathVariable String documentId) {
        return ResponseEntity.ok(portfolioManagementService.deleteDeliverableDocument(
                deliverableId,
                documentId,
                authentication.getName()));
    }

    @PostMapping("/programs/{programId}/open-issues")
    public ResponseEntity<OpenIssueResponse> createOpenIssue(
            Authentication authentication,
            @PathVariable String programId,
            @Valid @RequestBody CreateOpenIssueRequest request) {
        return ResponseEntity.ok(portfolioManagementService.createOpenIssue(programId, request, authentication.getName()));
    }
}
