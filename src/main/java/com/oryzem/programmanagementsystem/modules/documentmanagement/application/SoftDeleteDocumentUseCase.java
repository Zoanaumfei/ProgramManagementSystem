package com.oryzem.programmanagementsystem.modules.documentmanagement.application;

import com.oryzem.programmanagementsystem.modules.documentmanagement.application.model.read.DocumentReadModel;
import com.oryzem.programmanagementsystem.modules.documentmanagement.application.port.DocumentQueryRepository;
import com.oryzem.programmanagementsystem.modules.documentmanagement.application.port.DocumentRepository;
import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentPermission;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.shared.ResourceNotFoundException;
import java.time.Instant;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SoftDeleteDocumentUseCase {

    private final DocumentRecordLocator recordLocator;
    private final DocumentAuthorizationService authorizationService;
    private final DocumentRepository documentRepository;
    private final DocumentQueryRepository queryRepository;
    private final DocumentAuditService auditService;

    public SoftDeleteDocumentUseCase(
            DocumentRecordLocator recordLocator,
            DocumentAuthorizationService authorizationService,
            DocumentRepository documentRepository,
            DocumentQueryRepository queryRepository,
            DocumentAuditService auditService) {
        this.recordLocator = recordLocator;
        this.authorizationService = authorizationService;
        this.documentRepository = documentRepository;
        this.queryRepository = queryRepository;
        this.auditService = auditService;
    }

    @Transactional
    public DocumentView execute(String documentId, AuthenticatedUser actor) {
        DocumentRecordLocator.LocatedDocument located = recordLocator.require(documentId);
        authorizationService.authorizeContext(
                located.binding().contextType(),
                located.binding().contextId(),
                actor,
                DocumentPermission.DELETE_DOCUMENT);
        located.document().markDeleted(Instant.now());
        documentRepository.save(located.document());
        auditService.record(
                actor,
                "DOCUMENT_SOFT_DELETED",
                located.document().tenantId(),
                located.document().id(),
                located.binding().contextType(),
                located.binding().contextId(),
                "OK",
                Map.of("status", located.document().status().name()));
        DocumentReadModel readModel = queryRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", documentId));
        return readModel.toView();
    }
}