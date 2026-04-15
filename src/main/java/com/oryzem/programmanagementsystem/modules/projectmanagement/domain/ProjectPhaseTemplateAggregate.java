package com.oryzem.programmanagementsystem.modules.projectmanagement.domain;

public record ProjectPhaseTemplateAggregate(
        String id,
        String templateId,
        int sequenceNo,
        String name,
        String description,
        Integer plannedStartOffsetDays,
        int plannedEndOffsetDays) {

    public ProjectPhaseTemplateAggregate update(
            String nextName,
            String nextDescription,
            Integer nextPlannedStartOffsetDays,
            int nextPlannedEndOffsetDays) {
        return new ProjectPhaseTemplateAggregate(
                id,
                templateId,
                sequenceNo,
                nextName,
                nextDescription,
                nextPlannedStartOffsetDays,
                nextPlannedEndOffsetDays);
    }
}
