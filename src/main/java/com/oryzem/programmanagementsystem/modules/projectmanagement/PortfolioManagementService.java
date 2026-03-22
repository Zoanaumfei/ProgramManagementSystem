package com.oryzem.programmanagementsystem.modules.projectmanagement;

import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PortfolioManagementService {

    private final PortfolioMilestoneTemplateService milestoneTemplateService;
    private final PortfolioProgramService programService;
    private final PortfolioProjectService projectService;
    private final PortfolioExecutionService executionService;
    private final PortfolioGovernanceService governanceService;

    public PortfolioManagementService(
            PortfolioMilestoneTemplateService milestoneTemplateService,
            PortfolioProgramService programService,
            PortfolioProjectService projectService,
            PortfolioExecutionService executionService,
            PortfolioGovernanceService governanceService) {
        this.milestoneTemplateService = milestoneTemplateService;
        this.programService = programService;
        this.projectService = projectService;
        this.executionService = executionService;
        this.governanceService = governanceService;
    }

    @Transactional(readOnly = true)
    public List<MilestoneTemplateResponse> listMilestoneTemplates(AuthenticatedUser actor) {
        return milestoneTemplateService.listMilestoneTemplates(actor);
    }

    public MilestoneTemplateResponse createMilestoneTemplate(
            CreateMilestoneTemplateRequest request,
            AuthenticatedUser actor) {
        return milestoneTemplateService.createMilestoneTemplate(request, actor);
    }

    @Transactional(readOnly = true)
    public List<ProgramSummaryResponse> listPrograms(AuthenticatedUser actor, String ownerOrganizationId) {
        return programService.listPrograms(actor, ownerOrganizationId);
    }

    @Transactional(readOnly = true)
    public ProgramDetailResponse getProgram(String programId, AuthenticatedUser actor) {
        return programService.getProgram(programId, actor);
    }

    public ProgramDetailResponse createProgram(CreateProgramRequest request, AuthenticatedUser actor) {
        return programService.createProgram(request, actor);
    }

    public ProjectResponse createProject(String programId, CreateProjectRequest request, AuthenticatedUser actor) {
        return projectService.createProject(programId, request, actor);
    }

    public ProductResponse createProduct(String projectId, CreateProductRequest request, AuthenticatedUser actor) {
        return projectService.createProduct(projectId, request, actor);
    }

    public ItemResponse createItem(String productId, CreateItemRequest request, AuthenticatedUser actor) {
        return executionService.createItem(productId, request, actor);
    }

    public DeliverableResponse createDeliverable(String itemId, CreateDeliverableRequest request, AuthenticatedUser actor) {
        return executionService.createDeliverable(itemId, request, actor);
    }

    public OpenIssueResponse createOpenIssue(String programId, CreateOpenIssueRequest request, AuthenticatedUser actor) {
        return governanceService.createOpenIssue(programId, request, actor);
    }

    @Transactional(readOnly = true)
    public List<DeliverableDocumentResponse> listDeliverableDocuments(String deliverableId, AuthenticatedUser actor) {
        return executionService.listDeliverableDocuments(deliverableId, actor);
    }

    public DeliverableDocumentUploadResponse prepareDeliverableDocumentUpload(
            String deliverableId,
            PrepareDeliverableDocumentUploadRequest request,
            AuthenticatedUser actor) {
        return executionService.prepareDeliverableDocumentUpload(deliverableId, request, actor);
    }

    public DeliverableDocumentResponse completeDeliverableDocumentUpload(
            String deliverableId,
            String documentId,
            AuthenticatedUser actor) {
        return executionService.completeDeliverableDocumentUpload(deliverableId, documentId, actor);
    }

    public DeliverableDocumentDownloadResponse createDeliverableDocumentDownload(
            String deliverableId,
            String documentId,
            AuthenticatedUser actor) {
        return executionService.createDeliverableDocumentDownload(deliverableId, documentId, actor);
    }

    public DeliverableDocumentResponse deleteDeliverableDocument(
            String deliverableId,
            String documentId,
            AuthenticatedUser actor) {
        return executionService.deleteDeliverableDocument(deliverableId, documentId, actor);
    }
}
