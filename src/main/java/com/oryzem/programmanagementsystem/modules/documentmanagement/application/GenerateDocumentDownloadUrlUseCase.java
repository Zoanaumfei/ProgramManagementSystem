package com.oryzem.programmanagementsystem.modules.documentmanagement.application;

import com.oryzem.programmanagementsystem.modules.documentmanagement.config.DocumentManagementProperties;
import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentPermission;
import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentStatus;
import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentStorage;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.shared.BusinessRuleException;
import java.time.Duration;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GenerateDocumentDownloadUrlUseCase {

    private final DocumentRecordLocator recordLocator;
    private final DocumentAuthorizationService authorizationService;
    private final DocumentStorage documentStorage;
    private final DocumentManagementProperties properties;
    private final DocumentAuditService auditService;

    public GenerateDocumentDownloadUrlUseCase(
            DocumentRecordLocator recordLocator,
            DocumentAuthorizationService authorizationService,
            DocumentStorage documentStorage,
            DocumentManagementProperties properties,
            DocumentAuditService auditService) {
        this.recordLocator = recordLocator;
        this.authorizationService = authorizationService;
        this.documentStorage = documentStorage;
        this.properties = properties;
        this.auditService = auditService;
    }

    public DocumentDownloadLink execute(String documentId, AuthenticatedUser actor) {
        DocumentRecordLocator.LocatedDocument located = recordLocator.require(documentId);
        authorizationService.authorizeContext(
                located.binding().contextType(),
                located.binding().contextId(),
                actor,
                DocumentPermission.DOWNLOAD_DOCUMENT);
        if (located.document().status() != DocumentStatus.ACTIVE) {
            throw new BusinessRuleException(
                    "DOCUMENT_NOT_DOWNLOADABLE",
                    "Somente documentos ativos podem gerar URL de download.");
        }
        DocumentStorage.DownloadInstruction instruction = documentStorage.createDownloadInstruction(
                located.document().storageKey(),
                Duration.ofSeconds(properties.getDownloadUrlTtlSeconds()),
                located.document().originalFilename());
        auditService.record(
                actor,
                "DOCUMENT_DOWNLOAD_URL_REQUESTED",
                located.document().tenantId(),
                located.document().id(),
                located.binding().contextType(),
                located.binding().contextId(),
                "OK",
                Map.of("expiresAt", instruction.expiresAt().toString()));
        return new DocumentDownloadLink(instruction.url(), instruction.expiresAt());
    }
}