package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import com.oryzem.programmanagementsystem.modules.documentmanagement.application.DocumentPublicFacade;
import com.oryzem.programmanagementsystem.modules.documentmanagement.application.DocumentView;
import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentContextType;
import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentStatus;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.shared.BusinessRuleException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class ProjectDocumentValidationService {

    private final DocumentPublicFacade documentPublicFacade;

    public ProjectDocumentValidationService(DocumentPublicFacade documentPublicFacade) {
        this.documentPublicFacade = documentPublicFacade;
    }

    public List<String> validateSubmissionDocuments(
            ProjectAuthorizationService.DeliverableAccess access,
            String submissionId,
            List<String> documentIds,
            AuthenticatedUser actor) {
        if (documentIds == null || documentIds.isEmpty()) {
            return List.of();
        }

        Set<String> validatedIds = new LinkedHashSet<>();
        for (String documentId : documentIds) {
            if (documentId == null || documentId.isBlank()) {
                throw new BusinessRuleException(
                        "DOCUMENT_ID_REQUIRED",
                        "Every attached document must provide a non-blank document id.");
            }
            DocumentView document = documentPublicFacade.getAccessibleDocument(documentId, actor);
            assertActive(document);
            assertTenant(access.project().tenantId(), document);
            assertSubmissionContext(submissionId, document);
            validatedIds.add(document.id());
        }
        return List.copyOf(validatedIds);
    }

    private void assertActive(DocumentView document) {
        if (document.status() != DocumentStatus.ACTIVE) {
            throw new BusinessRuleException(
                    "DOCUMENT_NOT_ACTIVE",
                    "Only active documents can be attached to a deliverable submission.");
        }
    }

    private void assertTenant(String expectedTenantId, DocumentView document) {
        if (!expectedTenantId.equals(document.tenantId())) {
            throw new BusinessRuleException(
                    "DOCUMENT_TENANT_MISMATCH",
                    "The provided document belongs to a different tenant.");
        }
    }

    private void assertSubmissionContext(String submissionId, DocumentView document) {
        boolean matchesSubmissionContext = document.contextType() == DocumentContextType.PROJECT_DELIVERABLE_SUBMISSION
                && submissionId.equals(document.contextId());
        if (!matchesSubmissionContext) {
            throw new BusinessRuleException(
                    "DOCUMENT_CONTEXT_MISMATCH",
                    "The provided document is not attached to the expected deliverable submission context.");
        }
    }
}
