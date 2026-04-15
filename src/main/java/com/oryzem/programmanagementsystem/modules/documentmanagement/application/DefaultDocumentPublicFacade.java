package com.oryzem.programmanagementsystem.modules.documentmanagement.application;

import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import org.springframework.stereotype.Service;

@Service
public class DefaultDocumentPublicFacade implements DocumentPublicFacade {

    private final GetDocumentDetailUseCase getDocumentDetailUseCase;

    public DefaultDocumentPublicFacade(GetDocumentDetailUseCase getDocumentDetailUseCase) {
        this.getDocumentDetailUseCase = getDocumentDetailUseCase;
    }

    @Override
    public DocumentView getAccessibleDocument(String documentId, AuthenticatedUser actor) {
        return getDocumentDetailUseCase.execute(documentId, actor);
    }
}
