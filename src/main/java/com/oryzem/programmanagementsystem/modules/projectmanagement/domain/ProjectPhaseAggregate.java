package com.oryzem.programmanagementsystem.modules.projectmanagement.domain;

import java.time.LocalDate;

public record ProjectPhaseAggregate(
        String id,
        String projectId,
        String name,
        int sequenceNo,
        String status,
        LocalDate plannedStartDate,
        LocalDate plannedEndDate,
        LocalDate actualStartDate,
        LocalDate actualEndDate,
        long version) {

    public static ProjectPhaseAggregate create(
            String id,
            String projectId,
            String name,
            int sequenceNo,
            LocalDate plannedStartDate,
            LocalDate plannedEndDate) {
        return new ProjectPhaseAggregate(
                id,
                projectId,
                name,
                sequenceNo,
                "PLANNED",
                plannedStartDate,
                plannedEndDate,
                null,
                null,
                0L);
    }
}
