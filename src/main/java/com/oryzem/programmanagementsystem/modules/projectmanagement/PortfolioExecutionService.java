package com.oryzem.programmanagementsystem.modules.projectmanagement;

import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.documents.DocumentStorageObject;
import com.oryzem.programmanagementsystem.platform.documents.PortfolioDocumentProperties;
import com.oryzem.programmanagementsystem.platform.documents.PortfolioDocumentStorageGateway;
import com.oryzem.programmanagementsystem.platform.documents.PreparedDocumentDownload;
import com.oryzem.programmanagementsystem.platform.documents.PreparedDocumentUpload;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
class PortfolioExecutionService {

    private final ProductRepository productRepository;
    private final ItemRepository itemRepository;
    private final DeliverableRepository deliverableRepository;
    private final DeliverableDocumentRepository deliverableDocumentRepository;
    private final ProjectManagementLookupService lookupService;
    private final ProjectManagementAccessService accessService;
    private final PortfolioDocumentStorageGateway documentStorageGateway;
    private final PortfolioDocumentProperties documentProperties;

    PortfolioExecutionService(
            ProductRepository productRepository,
            ItemRepository itemRepository,
            DeliverableRepository deliverableRepository,
            DeliverableDocumentRepository deliverableDocumentRepository,
            ProjectManagementLookupService lookupService,
            ProjectManagementAccessService accessService,
            PortfolioDocumentStorageGateway documentStorageGateway,
            PortfolioDocumentProperties documentProperties) {
        this.productRepository = productRepository;
        this.itemRepository = itemRepository;
        this.deliverableRepository = deliverableRepository;
        this.deliverableDocumentRepository = deliverableDocumentRepository;
        this.lookupService = lookupService;
        this.accessService = accessService;
        this.documentStorageGateway = documentStorageGateway;
        this.documentProperties = documentProperties;
    }

    ItemResponse createItem(String productId, CreateItemRequest request, AuthenticatedUser actor) {
        if (itemRepository.existsByCodeIgnoreCase(request.code().trim())) {
            throw new IllegalArgumentException("Item code already exists.");
        }

        ProductEntity product = lookupService.findProduct(productId);
        accessService.assertCanManageTaskLayer(actor, product.getProject().getProgram().getOwnerOrganizationId());
        ItemEntity item = ItemEntity.create(
                actor.username(),
                product,
                request.name().trim(),
                request.code().trim().toUpperCase(),
                ProjectManagementValidationSupport.trimToNull(request.description()),
                ProjectManagementValidationSupport.defaultValue(request.status(), ItemStatus.ACTIVE));
        product.addItem(item, actor.username());
        productRepository.save(product);
        return ItemResponse.from(item);
    }

    DeliverableResponse createDeliverable(String itemId, CreateDeliverableRequest request, AuthenticatedUser actor) {
        ProjectManagementValidationSupport.validateDateRange(
                request.plannedDate(),
                request.dueDate(),
                "Deliverable");

        ItemEntity item = lookupService.findItem(itemId);
        accessService.assertCanManageTaskLayer(actor, item.getProduct().getProject().getProgram().getOwnerOrganizationId());
        DeliverableEntity deliverable = DeliverableEntity.create(
                actor.username(),
                item,
                request.name().trim(),
                ProjectManagementValidationSupport.trimToNull(request.description()),
                request.type(),
                ProjectManagementValidationSupport.defaultValue(request.status(), DeliverableStatus.PENDING),
                request.plannedDate(),
                request.dueDate());
        item.addDeliverable(deliverable, actor.username());
        itemRepository.save(item);
        return DeliverableResponse.from(deliverable);
    }

    List<DeliverableDocumentResponse> listDeliverableDocuments(String deliverableId, AuthenticatedUser actor) {
        DeliverableEntity deliverable = lookupService.findDeliverable(deliverableId);
        accessService.assertCanViewProgram(actor, deliverable.getItem().getProduct().getProject().getProgram());
        return deliverableDocumentRepository.findByDeliverableIdOrderByCreatedAtAsc(deliverableId).stream()
                .map(DeliverableDocumentResponse::from)
                .toList();
    }

    DeliverableDocumentUploadResponse prepareDeliverableDocumentUpload(
            String deliverableId,
            PrepareDeliverableDocumentUploadRequest request,
            AuthenticatedUser actor) {
        DeliverableEntity deliverable = lookupService.findDeliverable(deliverableId);
        accessService.assertCanManageTaskLayer(actor, deliverable.getItem().getProduct().getProject().getProgram().getOwnerOrganizationId());
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

        PreparedDocumentUpload preparedUpload = documentStorageGateway.prepareUpload(documentStorageObject(document));
        return new DeliverableDocumentUploadResponse(
                DeliverableDocumentResponse.from(document),
                preparedUpload.uploadUrl(),
                preparedUpload.expiresAt(),
                preparedUpload.requiredHeaders());
    }

    DeliverableDocumentResponse completeDeliverableDocumentUpload(
            String deliverableId,
            String documentId,
            AuthenticatedUser actor) {
        DeliverableDocumentEntity document = lookupService.findDeliverableDocument(deliverableId, documentId);
        accessService.assertCanManageTaskLayer(
                actor,
                document.getDeliverable().getItem().getProduct().getProject().getProgram().getOwnerOrganizationId());
        if (document.getStatus() == DeliverableDocumentStatus.DELETED) {
            throw new IllegalArgumentException("Deleted document cannot be completed.");
        }

        documentStorageGateway.assertObjectExists(documentStorageObject(document));
        document.markAvailable(actor.username());
        return DeliverableDocumentResponse.from(deliverableDocumentRepository.save(document));
    }

    DeliverableDocumentDownloadResponse createDeliverableDocumentDownload(
            String deliverableId,
            String documentId,
            AuthenticatedUser actor) {
        DeliverableDocumentEntity document = lookupService.findDeliverableDocument(deliverableId, documentId);
        accessService.assertCanViewProgram(actor, document.getDeliverable().getItem().getProduct().getProject().getProgram());
        if (document.getStatus() != DeliverableDocumentStatus.AVAILABLE) {
            throw new IllegalArgumentException("Only available documents can generate download URLs.");
        }

        PreparedDocumentDownload preparedDownload = documentStorageGateway.prepareDownload(documentStorageObject(document));
        return new DeliverableDocumentDownloadResponse(
                document.getId(),
                preparedDownload.downloadUrl(),
                preparedDownload.expiresAt());
    }

    DeliverableDocumentResponse deleteDeliverableDocument(
            String deliverableId,
            String documentId,
            AuthenticatedUser actor) {
        DeliverableDocumentEntity document = lookupService.findDeliverableDocument(deliverableId, documentId);
        accessService.assertCanManageTaskLayer(
                actor,
                document.getDeliverable().getItem().getProduct().getProject().getProgram().getOwnerOrganizationId());
        document.markDeleted(actor.username());
        return DeliverableDocumentResponse.from(deliverableDocumentRepository.save(document));
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
        String keyPrefix = normalizeKeyPrefix(documentProperties.keyPrefix());
        return "%s/organization/%s/program/%s/project/%s/deliverable/%s/document/%s/%s".formatted(
                keyPrefix,
                program.getOwnerOrganizationId(),
                program.getId(),
                project.getId(),
                deliverable.getId(),
                PortfolioIds.newId("BIN"),
                sanitizedFileName);
    }

    private String sanitizeFileName(String fileName) {
        return fileName.trim().replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String normalizeKeyPrefix(String keyPrefix) {
        String resolvedKeyPrefix = (keyPrefix == null || keyPrefix.isBlank())
                ? "portfolio"
                : keyPrefix.trim();
        return resolvedKeyPrefix.replaceAll("^/+", "").replaceAll("/+$", "");
    }

    private DocumentStorageObject documentStorageObject(DeliverableDocumentEntity document) {
        return new DocumentStorageObject(
                document.getStorageBucket(),
                document.getStorageKey(),
                document.getContentType(),
                document.getFileName());
    }
}
