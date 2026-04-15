package com.oryzem.programmanagementsystem.modules.documentmanagement.application;

import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentContextType;
import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentPermission;
import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentStatus;
import com.oryzem.programmanagementsystem.modules.documentmanagement.infrastructure.DocumentBindingEntity;
import com.oryzem.programmanagementsystem.modules.documentmanagement.infrastructure.DocumentEntity;
import com.oryzem.programmanagementsystem.modules.documentmanagement.infrastructure.SpringDataDocumentBindingJpaRepository;
import com.oryzem.programmanagementsystem.modules.documentmanagement.infrastructure.SpringDataDocumentJpaRepository;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ListDocumentsByContextUseCase {

    private final DocumentAuthorizationService authorizationService;
    private final SpringDataDocumentBindingJpaRepository bindingRepository;
    private final SpringDataDocumentJpaRepository documentRepository;
    private final DocumentViewMapper mapper;

    public ListDocumentsByContextUseCase(
            DocumentAuthorizationService authorizationService,
            SpringDataDocumentBindingJpaRepository bindingRepository,
            SpringDataDocumentJpaRepository documentRepository,
            DocumentViewMapper mapper) {
        this.authorizationService = authorizationService;
        this.bindingRepository = bindingRepository;
        this.documentRepository = documentRepository;
        this.mapper = mapper;
    }

    public List<DocumentView> execute(DocumentContextType contextType, String contextId, AuthenticatedUser actor) {
        authorizationService.authorizeContext(contextType, contextId, actor, DocumentPermission.VIEW_DOCUMENT);
        List<DocumentBindingEntity> bindings = bindingRepository.findAllByContextTypeAndContextIdOrderByCreatedAtDesc(contextType, contextId);
        Map<String, DocumentEntity> documentsById = new LinkedHashMap<>();
        documentRepository.findAllByIdIn(bindings.stream().map(DocumentBindingEntity::getDocumentId).toList())
                .forEach(document -> documentsById.put(document.getId(), document));
        return bindings.stream()
                .map(binding -> mapper.toView(documentsById.get(binding.getDocumentId()), binding))
                .filter(view -> view != null)
                .filter(view -> view.status() != DocumentStatus.DELETED)
                .toList();
    }
}
