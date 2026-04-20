package com.oryzem.programmanagementsystem.modules.documentmanagement.application;

import com.oryzem.programmanagementsystem.modules.documentmanagement.application.model.read.DocumentReadModel;
import com.oryzem.programmanagementsystem.modules.documentmanagement.application.port.DocumentQueryRepository;
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
    private final DocumentQueryRepository queryRepository;

    public GetDocumentDetailUseCase(
            DocumentRecordLocator recordLocator,
            DocumentAuthorizationService authorizationService,
            DocumentQueryRepository queryRepository) {
        this.recordLocator = recordLocator;
        this.authorizationService = authorizationService;
        this.queryRepository = queryRepository;
    }

    public DocumentView execute(String documentId, AuthenticatedUser actor) {
        DocumentRecordLocator.LocatedDocument located = recordLocator.require(documentId);
        authorizationService.authorizeContext(
                located.binding().contextType(),
                located.binding().contextId(),
                actor,
                DocumentPermission.VIEW_DOCUMENT);
        DocumentReadModel readModel = queryRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", documentId));
        if (readModel.status() == DocumentStatus.DELETED) {
            throw new ResourceNotFoundException("Document", documentId);
        }
        return readModel.toView();
    }
}