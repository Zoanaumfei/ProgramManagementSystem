package com.oryzem.programmanagementsystem.modules.documentmanagement.application;

import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentPermission;
import com.oryzem.programmanagementsystem.modules.documentmanagement.infrastructure.SpringDataDocumentJpaRepository;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import java.time.Instant;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SoftDeleteDocumentUseCase {

    private final DocumentRecordLocator recordLocator;
    private final DocumentAuthorizationService authorizationService;
    private final SpringDataDocumentJpaRepository documentRepository;
    private final DocumentViewMapper mapper;
    private final DocumentAuditService auditService;

    public SoftDeleteDocumentUseCase(
            DocumentRecordLocator recordLocator,
            DocumentAuthorizationService authorizationService,
            SpringDataDocumentJpaRepository documentRepository,
            DocumentViewMapper mapper,
            DocumentAuditService auditService) {
        this.recordLocator = recordLocator;
        this.authorizationService = authorizationService;
        this.documentRepository = documentRepository;
        this.mapper = mapper;
        this.auditService = auditService;
    }

    @Transactional
    public DocumentView execute(String documentId, AuthenticatedUser actor) {
        DocumentRecordLocator.LocatedDocument located = recordLocator.require(documentId);
        authorizationService.authorizeContext(
                located.binding().getContextType(),
                located.binding().getContextId(),
                actor,
                DocumentPermission.DELETE_DOCUMENT);
        located.document().markDeleted(Instant.now());
        documentRepository.save(located.document());
        auditService.record(
                actor,
                "DOCUMENT_SOFT_DELETED",
                located.document().getTenantId(),
                located.document().getId(),
                located.binding().getContextType(),
                located.binding().getContextId(),
                "OK",
                Map.of("status", located.document().getStatus().name()));
        return mapper.toView(located.document(), located.binding());
    }
}
