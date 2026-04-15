package com.oryzem.programmanagementsystem.modules.projectmanagement.application.model.authorization;

import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectPermission;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectVisibilityScope;

public record ProjectAuthorizationSnapshot(
        String tenantId,
        String projectId,
        String leadOrganizationId,
        ProjectVisibilityScope visibilityScope,
        ProjectActorContext actorContext,
        ProjectPermission permission) {
}
