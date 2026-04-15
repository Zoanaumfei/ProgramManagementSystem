package com.oryzem.programmanagementsystem.modules.projectmanagement.domain;

public record ProjectStructureLevelDefinition(
        String id,
        int sequence,
        boolean allowsChildren,
        boolean allowsMilestones,
        boolean allowsDeliverables) {
}
