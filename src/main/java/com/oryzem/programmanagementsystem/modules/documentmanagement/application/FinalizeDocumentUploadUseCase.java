package com.oryzem.programmanagementsystem.modules.documentmanagement.application;

import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentPermission;
import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentStatus;
import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentStorage;
import com.oryzem.programmanagementsystem.modules.documentmanagement.infrastructure.DocumentBindingEntity;
import com.oryzem.programmanagementsystem.modules.documentmanagement.infrastructure.DocumentEntity;
import com.oryzem.programmanagementsystem.modules.documentmanagement.infrastructure.SpringDataDocumentJpaRepository;
import com.oryzem.programmanagementsystem.modules.documentmanagement.support.DocumentFileSignatureValidator;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.shared.BusinessRuleException;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinalizeDocumentUploadUseCase {

    private final DocumentRecordLocator recordLocator;
    private final DocumentAuthorizationService authorizationService;
    private final SpringDataDocumentJpaRepository documentRepository;
    private final DocumentStorage documentStorage;
    private final DocumentFileSignatureValidator signatureValidator;
    private final DocumentViewMapper mapper;
    private final DocumentAuditService auditService;

    public FinalizeDocumentUploadUseCase(
            DocumentRecordLocator recordLocator,
            DocumentAuthorizationService authorizationService,
            SpringDataDocumentJpaRepository documentRepository,
            DocumentStorage documentStorage,
            DocumentFileSignatureValidator signatureValidator,
            DocumentViewMapper mapper,
            DocumentAuditService auditService) {
        this.recordLocator = recordLocator;
        this.authorizationService = authorizationService;
        this.documentRepository = documentRepository;
        this.documentStorage = documentStorage;
        this.signatureValidator = signatureValidator;
        this.mapper = mapper;
        this.auditService = auditService;
    }

    @Transactional(noRollbackFor = BusinessRuleException.class)
    public DocumentView execute(String documentId, AuthenticatedUser actor) {
        DocumentRecordLocator.LocatedDocument located = recordLocator.require(documentId);
        DocumentEntity document = located.document();
        DocumentBindingEntity binding = located.binding();

        authorizationService.authorizeContext(
                binding.getContextType(),
                binding.getContextId(),
                actor,
                DocumentPermission.UPLOAD_DOCUMENT);

        if (document.getStatus() == DocumentStatus.DELETED) {
            throw new BusinessRuleException(
                    "DOCUMENT_DELETED",
                    "O documento ja foi removido logicamente.");
        }
        if (document.getStatus() == DocumentStatus.ACTIVE) {
            return mapper.toView(document, binding);
        }

        Instant now = Instant.now();
        try {
            DocumentStorage.StoredObjectInfo objectInfo = documentStorage.headObject(document.getStorageKey());
            validateStoredObject(document, objectInfo);
            byte[] signatureBytes = documentStorage.readSignatureBytes(document.getStorageKey(), 32);
            signatureValidator.validate(document.getExtension(), signatureBytes);
            document.markActive(now);
            documentRepository.save(document);
            auditService.record(
                    actor,
                    "DOCUMENT_UPLOAD_FINALIZED",
                    document.getTenantId(),
                    document.getId(),
                    binding.getContextType(),
                    binding.getContextId(),
                    "OK",
                    Map.of("status", document.getStatus().name()));
            return mapper.toView(document, binding);
        } catch (RuntimeException exception) {
            document.markFailed(now);
            documentRepository.save(document);
            auditService.record(
                    actor,
                    "DOCUMENT_UPLOAD_FAILED",
                    document.getTenantId(),
                    document.getId(),
                    binding.getContextType(),
                    binding.getContextId(),
                    "FAILED",
                    Map.of("reason", exception.getMessage()));
            throw exception;
        }
    }

    private void validateStoredObject(DocumentEntity document, DocumentStorage.StoredObjectInfo objectInfo) {
        if (objectInfo == null || !objectInfo.exists()) {
            throw new BusinessRuleException(
                    "DOCUMENT_UPLOAD_OBJECT_NOT_FOUND",
                    "O objeto do upload nao foi encontrado no storage.");
        }
        if (objectInfo.sizeBytes() != document.getSizeBytes()) {
            throw new BusinessRuleException(
                    "DOCUMENT_UPLOAD_SIZE_MISMATCH",
                    "O tamanho do objeto armazenado nao corresponde ao metadata autorizado.");
        }
        String contentType = objectInfo.contentType() == null ? "" : objectInfo.contentType().toLowerCase(Locale.ROOT);
        if (!contentType.equals(document.getContentType().toLowerCase(Locale.ROOT))) {
            throw new BusinessRuleException(
                    "DOCUMENT_UPLOAD_CONTENT_TYPE_MISMATCH",
                    "O content type armazenado nao corresponde ao metadata autorizado.");
        }
        String checksum = objectInfo.metadata().getOrDefault("checksum-sha256", "");
        if (!document.getChecksumSha256().equals(checksum)) {
            throw new BusinessRuleException(
                    "DOCUMENT_UPLOAD_CHECKSUM_MISMATCH",
                    "O checksum armazenado nao corresponde ao metadata autorizado.");
        }
        String storedDocumentId = objectInfo.metadata().getOrDefault("document-id", "");
        if (!document.getId().equals(storedDocumentId)) {
            throw new BusinessRuleException(
                    "DOCUMENT_UPLOAD_METADATA_MISMATCH",
                    "O objeto armazenado nao esta associado ao documentId esperado.");
        }
    }
}
