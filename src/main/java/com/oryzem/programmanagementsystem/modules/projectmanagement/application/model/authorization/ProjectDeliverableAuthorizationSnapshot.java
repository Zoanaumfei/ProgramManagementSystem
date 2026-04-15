package com.oryzem.programmanagementsystem.modules.projectmanagement.application.model.authorization;

import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectDeliverableStatus;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectPermission;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectVisibilityScope;

public record ProjectDeliverableAuthorizationSnapshot(
        String tenantId,
        String projectId,
        String deliverableId,
        String leadOrganizationId,
        ProjectVisibilityScope visibilityScope,
        String responsibleOrganizationId,
        String responsibleUserId,
        String approverOrganizationId,
        String approverUserId,
        ProjectDeliverableStatus status,
        ProjectActorContext actorContext,
        ProjectPermission permission) {
}
