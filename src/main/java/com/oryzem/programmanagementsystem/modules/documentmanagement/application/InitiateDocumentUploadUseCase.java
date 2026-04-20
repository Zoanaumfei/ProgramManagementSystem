package com.oryzem.programmanagementsystem.modules.documentmanagement.application;

import com.oryzem.programmanagementsystem.modules.documentmanagement.application.model.DocumentBindingRecord;
import com.oryzem.programmanagementsystem.modules.documentmanagement.application.model.DocumentRecord;
import com.oryzem.programmanagementsystem.modules.documentmanagement.application.port.DocumentBindingRepository;
import com.oryzem.programmanagementsystem.modules.documentmanagement.application.port.DocumentRepository;
import com.oryzem.programmanagementsystem.modules.documentmanagement.config.DocumentManagementProperties;
import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentContextType;
import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentPermission;
import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentStorage;
import com.oryzem.programmanagementsystem.modules.documentmanagement.support.DocumentFilePolicy;
import com.oryzem.programmanagementsystem.modules.documentmanagement.support.DocumentIds;
import com.oryzem.programmanagementsystem.modules.documentmanagement.support.DocumentStorageKeyFactory;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.shared.BusinessRuleException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InitiateDocumentUploadUseCase {

    private final DocumentAuthorizationService authorizationService;
    private final DocumentFilePolicy documentFilePolicy;
    private final DocumentManagementProperties properties;
    private final DocumentRepository documentRepository;
    private final DocumentBindingRepository bindingRepository;
    private final DocumentStorageKeyFactory storageKeyFactory;
    private final DocumentStorage documentStorage;
    private final DocumentAuditService auditService;

    public InitiateDocumentUploadUseCase(
            DocumentAuthorizationService authorizationService,
            DocumentFilePolicy documentFilePolicy,
            DocumentManagementProperties properties,
            DocumentRepository documentRepository,
            DocumentBindingRepository bindingRepository,
            DocumentStorageKeyFactory storageKeyFactory,
            DocumentStorage documentStorage,
            DocumentAuditService auditService) {
        this.authorizationService = authorizationService;
        this.documentFilePolicy = documentFilePolicy;
        this.properties = properties;
        this.documentRepository = documentRepository;
        this.bindingRepository = bindingRepository;
        this.storageKeyFactory = storageKeyFactory;
        this.documentStorage = documentStorage;
        this.auditService = auditService;
    }

    @Transactional
    public DocumentUploadSession execute(
            DocumentContextType contextType,
            String contextId,
            String originalFilename,
            String contentType,
            long sizeBytes,
            String checksumSha256,
            AuthenticatedUser actor) {
        DocumentContextPolicyRegistry.ResolvedDocumentContext resolvedContext = authorizationService.authorizeContext(
                contextType,
                contextId,
                actor,
                DocumentPermission.UPLOAD_DOCUMENT);
        DocumentFilePolicy.ValidatedDocumentFile validatedFile = documentFilePolicy.validate(
                originalFilename,
                contentType,
                sizeBytes,
                checksumSha256);

        long trackedDocuments = bindingRepository.countTrackedByContext(contextType, contextId);
        if (trackedDocuments >= properties.getMaxFilesPerContext()) {
            throw new BusinessRuleException(
                    "DOCUMENT_CONTEXT_LIMIT_REACHED",
                    "O contexto atingiu o limite configurado de arquivos.",
                    Map.of("maxFilesPerContext", properties.getMaxFilesPerContext()));
        }

        Instant now = Instant.now();
        String documentId = DocumentIds.newDocumentId();
        String storageKey = storageKeyFactory.create(
                resolvedContext.ownerTenantId(),
                contextType,
                contextId,
                documentId);

        DocumentRecord document = DocumentRecord.initiate(
                documentId,
                resolvedContext.ownerTenantId(),
                validatedFile.originalFilename(),
                validatedFile.safeFilename(),
                validatedFile.contentType(),
                validatedFile.extension(),
                validatedFile.sizeBytes(),
                validatedFile.checksumSha256(),
                properties.getStorage().getProvider().name(),
                storageKey,
                actor.userId(),
                actor.organizationId(),
                now.plus(Duration.ofMinutes(properties.getPendingUploadExpirationMinutes())),
                now);
        DocumentBindingRecord binding = DocumentBindingRecord.create(
                DocumentIds.newBindingId(),
                documentId,
                contextType,
                contextId,
                resolvedContext.policy().ownerOrganizationId(),
                actor.userId(),
                now);

        documentRepository.save(document);
        bindingRepository.save(binding);

        try {
            DocumentStorage.UploadInstruction instruction = documentStorage.createUploadInstruction(
                    storageKey,
                    validatedFile.contentType(),
                    validatedFile.sizeBytes(),
                    validatedFile.checksumSha256(),
                    documentId,
                    Duration.ofSeconds(properties.getUploadUrlTtlSeconds()));

            auditService.record(
                    actor,
                    "DOCUMENT_UPLOAD_INITIATED",
                    document.tenantId(),
                    documentId,
                    contextType,
                    contextId,
                    "OK",
                    Map.of("sizeBytes", validatedFile.sizeBytes(), "contentType", validatedFile.contentType()));

            return new DocumentUploadSession(documentId, instruction.url(), instruction.fields(), instruction.expiresAt());
        } catch (RuntimeException exception) {
            document.markFailed(Instant.now());
            documentRepository.save(document);
            auditService.record(
                    actor,
                    "DOCUMENT_UPLOAD_FAILED",
                    document.tenantId(),
                    documentId,
                    contextType,
                    contextId,
                    "FAILED",
                    Map.of("reason", exception.getMessage()));
            throw exception;
        }
    }
}