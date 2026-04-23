package com.oryzem.programmanagementsystem.modules.projectmanagement.domain;

import java.time.Instant;

public record ProjectStructureTemplateAggregate(
        String id,
        String name,
        String frameworkType,
        int version,
        boolean active,
        String ownerOrganizationId,
        Instant createdAt) {

    public ProjectStructureTemplateAggregate rename(String nextName) {
        return new ProjectStructureTemplateAggregate(
                id,
                nextName,
                frameworkType,
                version,
                active,
                ownerOrganizationId,
                createdAt);
    }

    public ProjectStructureTemplateAggregate withActive(boolean nextActive) {
        return new ProjectStructureTemplateAggregate(
                id,
                name,
                frameworkType,
                version,
                nextActive,
                ownerOrganizationId,
                createdAt);
    }
}
