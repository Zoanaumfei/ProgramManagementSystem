package com.oryzem.programmanagementsystem.modules.projectmanagement.domain;

import java.time.Instant;

public record ProjectMemberAggregate(
        String id,
        String projectId,
        String userId,
        String organizationId,
        ProjectMemberRole projectRole,
        boolean active,
        Instant assignedAt) {
}
