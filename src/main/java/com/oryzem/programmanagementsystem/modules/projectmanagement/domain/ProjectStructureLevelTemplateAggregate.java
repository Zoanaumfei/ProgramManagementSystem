package com.oryzem.programmanagementsystem.modules.projectmanagement.domain;

public record ProjectStructureLevelTemplateAggregate(
        String id,
        String structureTemplateId,
        int sequenceNo,
        String name,
        String code,
        boolean allowsChildren,
        boolean allowsMilestones,
        boolean allowsDeliverables) {

    public ProjectStructureLevelTemplateAggregate update(
            String nextName,
            String nextCode,
            boolean nextAllowsChildren,
            boolean nextAllowsMilestones,
            boolean nextAllowsDeliverables) {
        return new ProjectStructureLevelTemplateAggregate(
                id,
                structureTemplateId,
                sequenceNo,
                nextName,
                nextCode,
                nextAllowsChildren,
                nextAllowsMilestones,
                nextAllowsDeliverables);
    }

    public ProjectStructureLevelTemplateAggregate withSequence(int nextSequenceNo) {
        return new ProjectStructureLevelTemplateAggregate(
                id,
                structureTemplateId,
                nextSequenceNo,
                name,
                code,
                allowsChildren,
                allowsMilestones,
                allowsDeliverables);
    }
}
