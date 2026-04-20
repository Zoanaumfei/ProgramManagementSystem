package com.oryzem.programmanagementsystem.modules.documentmanagement.application;

import com.oryzem.programmanagementsystem.modules.documentmanagement.application.port.DocumentQueryRepository;
import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentContextType;
import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentPermission;
import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentStatus;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ListDocumentsByContextUseCase {

    private final DocumentAuthorizationService authorizationService;
    private final DocumentQueryRepository queryRepository;

    public ListDocumentsByContextUseCase(
            DocumentAuthorizationService authorizationService,
            DocumentQueryRepository queryRepository) {
        this.authorizationService = authorizationService;
        this.queryRepository = queryRepository;
    }

    public List<DocumentView> execute(DocumentContextType contextType, String contextId, AuthenticatedUser actor) {
        authorizationService.authorizeContext(contextType, contextId, actor, DocumentPermission.VIEW_DOCUMENT);
        return queryRepository.findAllByContextTypeAndContextIdOrderByCreatedAtDesc(contextType, contextId).stream()
                .filter(view -> view.status() != DocumentStatus.DELETED)
                .map(readModel -> readModel.toView())
                .toList();
    }
}