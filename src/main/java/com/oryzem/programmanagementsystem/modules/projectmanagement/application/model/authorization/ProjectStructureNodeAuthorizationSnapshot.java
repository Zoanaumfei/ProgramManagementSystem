package com.oryzem.programmanagementsystem.modules.projectmanagement.application.model.authorization;

import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectPermission;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectVisibilityScope;

public record ProjectStructureNodeAuthorizationSnapshot(
        String tenantId,
        String projectId,
        String nodeId,
        String leadOrganizationId,
        ProjectVisibilityScope visibilityScope,
        String ownerOrganizationId,
        String responsibleUserId,
        ProjectActorContext actorContext,
        ProjectPermission permission) {
}
