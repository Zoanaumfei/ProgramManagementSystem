package com.oryzem.programmanagementsystem.modules.projectmanagement.application.model.authorization;

import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectPermission;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectVisibilityScope;

public record ProjectMilestoneAuthorizationSnapshot(
        String tenantId,
        String projectId,
        String milestoneId,
        String leadOrganizationId,
        ProjectVisibilityScope visibilityScope,
        String ownerOrganizationId,
        ProjectActorContext actorContext,
        ProjectPermission permission) {
}
