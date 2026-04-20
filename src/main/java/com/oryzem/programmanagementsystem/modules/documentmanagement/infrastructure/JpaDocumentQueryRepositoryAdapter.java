package com.oryzem.programmanagementsystem.modules.documentmanagement.infrastructure;

import com.oryzem.programmanagementsystem.modules.documentmanagement.application.model.read.DocumentReadModel;
import com.oryzem.programmanagementsystem.modules.documentmanagement.application.port.DocumentQueryRepository;
import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentContextType;
import com.oryzem.programmanagementsystem.modules.documentmanagement.infrastructure.mapper.DocumentReadModelMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaDocumentQueryRepositoryAdapter implements DocumentQueryRepository {

    private final SpringDataDocumentJpaRepository documentRepository;
    private final SpringDataDocumentBindingJpaRepository bindingRepository;
    private final DocumentReadModelMapper mapper;

    public JpaDocumentQueryRepositoryAdapter(
            SpringDataDocumentJpaRepository documentRepository,
            SpringDataDocumentBindingJpaRepository bindingRepository,
            DocumentReadModelMapper mapper) {
        this.documentRepository = documentRepository;
        this.bindingRepository = bindingRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<DocumentReadModel> findById(String documentId) {
        return documentRepository.findById(documentId)
                .flatMap(document -> bindingRepository.findByDocumentId(documentId)
                        .map(binding -> mapper.toReadModel(document, binding)));
    }

    @Override
    public List<DocumentReadModel> findAllByContextTypeAndContextIdOrderByCreatedAtDesc(
            DocumentContextType contextType,
            String contextId) {
        List<DocumentBindingEntity> bindings = bindingRepository.findAllByContextTypeAndContextIdOrderByCreatedAtDesc(contextType, contextId);
        Map<String, DocumentEntity> documentsById = new LinkedHashMap<>();
        documentRepository.findAllByIdIn(bindings.stream().map(DocumentBindingEntity::getDocumentId).toList())
                .forEach(document -> documentsById.put(document.getId(), document));
        return bindings.stream()
                .map(binding -> mapper.toReadModel(documentsById.get(binding.getDocumentId()), binding))
                .filter(readModel -> readModel != null)
                .toList();
    }
}