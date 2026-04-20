package com.oryzem.programmanagementsystem.modules.documentmanagement.application;

import com.oryzem.programmanagementsystem.modules.documentmanagement.application.model.DocumentBindingRecord;
import com.oryzem.programmanagementsystem.modules.documentmanagement.application.model.DocumentRecord;
import com.oryzem.programmanagementsystem.modules.documentmanagement.application.model.read.DocumentReadModel;
import com.oryzem.programmanagementsystem.modules.documentmanagement.application.port.DocumentQueryRepository;
import com.oryzem.programmanagementsystem.modules.documentmanagement.application.port.DocumentRepository;
import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentPermission;
import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentStatus;
import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentStorage;
import com.oryzem.programmanagementsystem.modules.documentmanagement.support.DocumentFileSignatureValidator;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.shared.BusinessRuleException;
import com.oryzem.programmanagementsystem.platform.shared.ResourceNotFoundException;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinalizeDocumentUploadUseCase {

    private final DocumentRecordLocator recordLocator;
    private final DocumentAuthorizationService authorizationService;
    private final DocumentRepository documentRepository;
    private final DocumentQueryRepository queryRepository;
    private final DocumentStorage documentStorage;
    private final DocumentFileSignatureValidator signatureValidator;
    private final DocumentAuditService auditService;

    public FinalizeDocumentUploadUseCase(
            DocumentRecordLocator recordLocator,
            DocumentAuthorizationService authorizationService,
            DocumentRepository documentRepository,
            DocumentQueryRepository queryRepository,
            DocumentStorage documentStorage,
            DocumentFileSignatureValidator signatureValidator,
            DocumentAuditService auditService) {
        this.recordLocator = recordLocator;
        this.authorizationService = authorizationService;
        this.documentRepository = documentRepository;
        this.queryRepository = queryRepository;
        this.documentStorage = documentStorage;
        this.signatureValidator = signatureValidator;
        this.auditService = auditService;
    }

    @Transactional(noRollbackFor = BusinessRuleException.class)
    public DocumentView execute(String documentId, AuthenticatedUser actor) {
        DocumentRecordLocator.LocatedDocument located = recordLocator.require(documentId);
        DocumentRecord document = located.document();
        DocumentBindingRecord binding = located.binding();

        authorizationService.authorizeContext(
                binding.contextType(),
                binding.contextId(),
                actor,
                DocumentPermission.UPLOAD_DOCUMENT);

        if (document.status() == DocumentStatus.DELETED) {
            throw new BusinessRuleException(
                    "DOCUMENT_DELETED",
                    "O documento ja foi removido logicamente.");
        }
        if (document.status() == DocumentStatus.ACTIVE) {
            return requireView(documentId);
        }

        Instant now = Instant.now();
        try {
            DocumentStorage.StoredObjectInfo objectInfo = documentStorage.headObject(document.storageKey());
            validateStoredObject(document, objectInfo);
            byte[] signatureBytes = documentStorage.readSignatureBytes(document.storageKey(), 32);
            signatureValidator.validate(document.extension(), signatureBytes);
            document.markActive(now);
            documentRepository.save(document);
            auditService.record(
                    actor,
                    "DOCUMENT_UPLOAD_FINALIZED",
                    document.tenantId(),
                    document.id(),
                    binding.contextType(),
                    binding.contextId(),
                    "OK",
                    Map.of("status", document.status().name()));
            return requireView(documentId);
        } catch (RuntimeException exception) {
            document.markFailed(now);
            documentRepository.save(document);
            auditService.record(
                    actor,
                    "DOCUMENT_UPLOAD_FAILED",
                    document.tenantId(),
                    document.id(),
                    binding.contextType(),
                    binding.contextId(),
                    "FAILED",
                    Map.of("reason", exception.getMessage()));
            throw exception;
        }
    }

    private DocumentView requireView(String documentId) {
        DocumentReadModel readModel = queryRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", documentId));
        return readModel.toView();
    }

    private void validateStoredObject(DocumentRecord document, DocumentStorage.StoredObjectInfo objectInfo) {
        if (objectInfo == null || !objectInfo.exists()) {
            throw new BusinessRuleException(
                    "DOCUMENT_UPLOAD_OBJECT_NOT_FOUND",
                    "O objeto do upload nao foi encontrado no storage.");
        }
        if (objectInfo.sizeBytes() != document.sizeBytes()) {
            throw new BusinessRuleException(
                    "DOCUMENT_UPLOAD_SIZE_MISMATCH",
                    "O tamanho do objeto armazenado nao corresponde ao metadata autorizado.");
        }
        String contentType = objectInfo.contentType() == null ? "" : objectInfo.contentType().toLowerCase(Locale.ROOT);
        if (!contentType.equals(document.contentType().toLowerCase(Locale.ROOT))) {
            throw new BusinessRuleException(
                    "DOCUMENT_UPLOAD_CONTENT_TYPE_MISMATCH",
                    "O content type armazenado nao corresponde ao metadata autorizado.");
        }
        String checksum = objectInfo.metadata().getOrDefault("checksum-sha256", "");
        if (!document.checksumSha256().equals(checksum)) {
            throw new BusinessRuleException(
                    "DOCUMENT_UPLOAD_CHECKSUM_MISMATCH",
                    "O checksum armazenado nao corresponde ao metadata autorizado.");
        }
        String storedDocumentId = objectInfo.metadata().getOrDefault("document-id", "");
        if (!document.id().equals(storedDocumentId)) {
            throw new BusinessRuleException(
                    "DOCUMENT_UPLOAD_METADATA_MISMATCH",
                    "O objeto armazenado nao esta associado ao documentId esperado.");
        }
    }
}