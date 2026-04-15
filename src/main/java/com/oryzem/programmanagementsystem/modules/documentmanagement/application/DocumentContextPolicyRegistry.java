package com.oryzem.programmanagementsystem.modules.documentmanagement.application;

import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentContextPolicy;
import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentContextPolicyProvider;
import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentContextType;
import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentPermission;
import com.oryzem.programmanagementsystem.platform.access.AccessContextService;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.authorization.TenantType;
import com.oryzem.programmanagementsystem.platform.shared.BusinessRuleException;
import com.oryzem.programmanagementsystem.platform.shared.ResourceNotFoundException;
import com.oryzem.programmanagementsystem.platform.tenant.OrganizationBoundaryResolver;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class DocumentContextPolicyRegistry {

    private final Map<DocumentContextType, DocumentContextPolicyProvider> providers;
    private final OrganizationBoundaryResolver organizationBoundaryResolver;
    private final AccessContextService accessContextService;

    public DocumentContextPolicyRegistry(
            List<DocumentContextPolicyProvider> providers,
            OrganizationBoundaryResolver organizationBoundaryResolver,
            AccessContextService accessContextService) {
        this.providers = new EnumMap<>(DocumentContextType.class);
        providers.forEach(provider -> this.providers.put(provider.supports(), provider));
        this.organizationBoundaryResolver = organizationBoundaryResolver;
        this.accessContextService = accessContextService;
    }

    public ResolvedDocumentContext authorize(
            DocumentContextType contextType,
            String contextId,
            AuthenticatedUser actor,
            DocumentPermission permission) {
        DocumentContextPolicyProvider provider = providers.get(contextType);
        if (provider == null) {
            throw new AccessDeniedException(
                    "Nao existe provider de policy documental configurado para o contexto informado.");
        }
        DocumentContextPolicy policy = provider.resolve(contextId, actor);
        if (policy == null || !policy.exists()) {
            throw new ResourceNotFoundException("Document context", contextType.name() + ":" + contextId);
        }
        if (!policy.acceptsDocuments()) {
            throw new BusinessRuleException(
                    "DOCUMENT_CONTEXT_REJECTS_DOCUMENTS",
                    "O contexto informado nao aceita documentos neste momento.");
        }
        if (!policy.featureEnabled()) {
            throw new AccessDeniedException("A feature de documentos nao esta habilitada para o contexto informado.");
        }
        if (policy.ownerOrganizationId() == null || policy.ownerOrganizationId().isBlank()) {
            throw new IllegalStateException("Document context policy must expose an owner organization.");
        }

        OrganizationBoundaryResolver.OrganizationBoundaryView ownerBoundary = organizationBoundaryResolver.findBoundary(policy.ownerOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Owner organization", policy.ownerOrganizationId()));

        if (actor == null || actor.tenantId() == null || actor.tenantId().isBlank()) {
            throw new AccessDeniedException("Nao foi possivel resolver o tenant ativo do usuario autenticado.");
        }

        String actorTenantId = accessContextService.canonicalTenantId(actor.tenantId());
        String ownerTenantId = accessContextService.canonicalTenantId(ownerBoundary.tenantId());
        if (actor.tenantType() != TenantType.INTERNAL && ownerTenantId != null && !ownerTenantId.equals(actorTenantId)) {
            throw new AccessDeniedException("O contexto documental informado pertence a outro tenant.");
        }
        if (!policy.isAllowed(permission)) {
            throw new AccessDeniedException("O usuario autenticado nao possui permissao documental para a operacao solicitada.");
        }
        return new ResolvedDocumentContext(contextType, contextId, policy, ownerTenantId);
    }

    public record ResolvedDocumentContext(
            DocumentContextType contextType,
            String contextId,
            DocumentContextPolicy policy,
            String ownerTenantId) {
    }
}
