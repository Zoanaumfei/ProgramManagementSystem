package com.oryzem.programmanagementsystem.modules.documentmanagement.application;

import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentPermission;
import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentStatus;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.shared.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GetDocumentDetailUseCase {

    private final DocumentRecordLocator recordLocator;
    private final DocumentAuthorizationService authorizationService;
    private final DocumentViewMapper mapper;

    public GetDocumentDetailUseCase(
            DocumentRecordLocator recordLocator,
            DocumentAuthorizationService authorizationService,
            DocumentViewMapper mapper) {
        this.recordLocator = recordLocator;
        this.authorizationService = authorizationService;
        this.mapper = mapper;
    }

    public DocumentView execute(String documentId, AuthenticatedUser actor) {
        DocumentRecordLocator.LocatedDocument located = recordLocator.require(documentId);
        authorizationService.authorizeContext(
                located.binding().getContextType(),
                located.binding().getContextId(),
                actor,
                DocumentPermission.VIEW_DOCUMENT);
        if (located.document().getStatus() == DocumentStatus.DELETED) {
            throw new ResourceNotFoundException("Document", documentId);
        }
        return mapper.toView(located.document(), located.binding());
    }
}
