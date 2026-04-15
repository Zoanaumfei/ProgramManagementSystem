package com.oryzem.programmanagementsystem.modules.projectmanagement.domain;

import java.time.Instant;

public record ProjectTemplateAggregate(
        String id,
        String name,
        ProjectFrameworkType frameworkType,
        int version,
        ProjectTemplateStatus status,
        String structureTemplateId,
        boolean isDefault,
        Instant createdAt) {

    public ProjectTemplateAggregate update(
            String nextName,
            ProjectTemplateStatus nextStatus,
            String nextStructureTemplateId,
            boolean nextIsDefault) {
        return new ProjectTemplateAggregate(
                id,
                nextName,
                frameworkType,
                version,
                nextStatus,
                nextStructureTemplateId,
                nextIsDefault,
                createdAt);
    }

    public ProjectTemplateAggregate withDefault(boolean nextIsDefault) {
        return new ProjectTemplateAggregate(
                id,
                name,
                frameworkType,
                version,
                status,
                structureTemplateId,
                nextIsDefault,
                createdAt);
    }
}
