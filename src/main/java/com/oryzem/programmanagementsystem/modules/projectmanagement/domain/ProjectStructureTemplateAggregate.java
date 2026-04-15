package com.oryzem.programmanagementsystem.modules.projectmanagement.domain;

import java.time.Instant;

public record ProjectStructureTemplateAggregate(
        String id,
        String name,
        ProjectFrameworkType frameworkType,
        int version,
        boolean active,
        Instant createdAt) {

    public ProjectStructureTemplateAggregate rename(String nextName) {
        return new ProjectStructureTemplateAggregate(
                id,
                nextName,
                frameworkType,
                version,
                active,
                createdAt);
    }

    public ProjectStructureTemplateAggregate withActive(boolean nextActive) {
        return new ProjectStructureTemplateAggregate(
                id,
                name,
                frameworkType,
                version,
                nextActive,
                createdAt);
    }
}
