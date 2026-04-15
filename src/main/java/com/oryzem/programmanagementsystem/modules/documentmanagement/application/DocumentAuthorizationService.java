package com.oryzem.programmanagementsystem.modules.documentmanagement.application;

import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentContextType;
import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentPermission;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import java.util.Map;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class DocumentAuthorizationService {

    private final DocumentContextPolicyRegistry registry;
    private final DocumentAuditService auditService;

    public DocumentAuthorizationService(
            DocumentContextPolicyRegistry registry,
            DocumentAuditService auditService) {
        this.registry = registry;
        this.auditService = auditService;
    }

    public DocumentContextPolicyRegistry.ResolvedDocumentContext authorizeContext(
            DocumentContextType contextType,
            String contextId,
            AuthenticatedUser actor,
            DocumentPermission permission) {
        try {
            return registry.authorize(contextType, contextId, actor, permission);
        } catch (AccessDeniedException exception) {
            auditService.record(
                    actor,
                    "DOCUMENT_ACCESS_DENIED",
                    actor != null ? actor.tenantId() : null,
                    null,
                    contextType,
                    contextId,
                    "DENIED",
                    Map.of("permission", permission.name(), "reason", exception.getMessage()));
            throw exception;
        }
    }
}
