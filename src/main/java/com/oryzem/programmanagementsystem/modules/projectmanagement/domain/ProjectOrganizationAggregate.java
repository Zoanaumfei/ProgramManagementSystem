package com.oryzem.programmanagementsystem.modules.projectmanagement.domain;

import java.time.Instant;

public record ProjectOrganizationAggregate(
        String id,
        String projectId,
        String organizationId,
        ProjectOrganizationRoleType roleType,
        Instant joinedAt,
        boolean active) {
}
