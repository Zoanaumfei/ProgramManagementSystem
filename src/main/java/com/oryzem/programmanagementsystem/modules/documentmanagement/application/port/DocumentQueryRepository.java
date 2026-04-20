package com.oryzem.programmanagementsystem.modules.documentmanagement.application.port;

import com.oryzem.programmanagementsystem.modules.documentmanagement.application.model.read.DocumentReadModel;
import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentContextType;
import java.util.List;
import java.util.Optional;

public interface DocumentQueryRepository {

    Optional<DocumentReadModel> findById(String documentId);

    List<DocumentReadModel> findAllByContextTypeAndContextIdOrderByCreatedAtDesc(
            DocumentContextType contextType,
            String contextId);
}