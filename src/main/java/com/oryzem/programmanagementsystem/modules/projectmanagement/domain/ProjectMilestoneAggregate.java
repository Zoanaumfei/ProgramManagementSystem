package com.oryzem.programmanagementsystem.modules.projectmanagement.domain;

import com.oryzem.programmanagementsystem.platform.shared.BusinessRuleException;
import java.time.LocalDate;
import java.util.Map;

public record ProjectMilestoneAggregate(
        String id,
        String projectId,
        String structureNodeId,
        String phaseId,
        String code,
        String name,
        int sequence,
        LocalDate plannedDate,
        LocalDate actualDate,
        ProjectMilestoneStatus status,
        String ownerOrganizationId,
        ProjectVisibilityScope visibilityScope,
        long version) {

    public ProjectMilestoneAggregate update(
            LocalDate nextPlannedDate,
            LocalDate nextActualDate,
            ProjectMilestoneStatus nextStatus,
            String nextOwnerOrganizationId,
            ProjectVisibilityScope nextVisibilityScope) {
        if (nextActualDate != null && nextPlannedDate != null && nextActualDate.isBefore(nextPlannedDate.minusYears(10))) {
            throw new BusinessRuleException(
                    "PROJECT_MILESTONE_DATE_INVALID",
                    "Milestone actual date is outside the expected range.",
                    Map.of("milestoneId", id));
        }
        return new ProjectMilestoneAggregate(
                id,
                projectId,
                structureNodeId,
                phaseId,
                code,
                name,
                sequence,
                nextPlannedDate,
                nextActualDate,
                nextStatus != null ? nextStatus : status,
                nextOwnerOrganizationId,
                nextVisibilityScope != null ? nextVisibilityScope : visibilityScope,
                version);
    }
}
