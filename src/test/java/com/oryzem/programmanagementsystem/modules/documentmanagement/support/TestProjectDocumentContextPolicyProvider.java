package com.oryzem.programmanagementsystem.modules.documentmanagement.support;

import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentContextPolicy;
import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentContextPolicyProvider;
import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentContextType;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;

public class TestProjectDocumentContextPolicyProvider implements DocumentContextPolicyProvider {

    @Override
    public DocumentContextType supports() {
        return DocumentContextType.PROJECT;
    }

    @Override
    public DocumentContextPolicy resolve(String contextId, AuthenticatedUser actor) {
        return switch (contextId) {
            case "project-123", "project-list" -> tenantAPolicy(true, true, true, true);
            case "project-no-download" -> tenantAPolicy(true, true, false, false);
            case "project-disabled" -> new DocumentContextPolicy(true, true, false, "tenant-a", true, true, true, false);
            case "project-read-only" -> tenantAPolicy(true, false, true, false);
            case "project-tenant-b" -> new DocumentContextPolicy(true, true, true, "tenant-b", true, true, true, true);
            case "project-archived" -> new DocumentContextPolicy(true, false, true, "tenant-a", true, false, false, false);
            default -> new DocumentContextPolicy(false, false, false, null, false, false, false, false);
        };
    }

    private DocumentContextPolicy tenantAPolicy(
            boolean canView,
            boolean canUpload,
            boolean canDownload,
            boolean canDelete) {
        return new DocumentContextPolicy(true, true, true, "tenant-a", canView, canUpload, canDownload, canDelete);
    }
}
