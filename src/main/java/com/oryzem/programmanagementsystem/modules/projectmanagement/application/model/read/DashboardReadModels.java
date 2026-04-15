package com.oryzem.programmanagementsystem.modules.projectmanagement.application.model.read;

import java.time.LocalDate;

public final class DashboardReadModels {

    private DashboardReadModels() {
    }

    public record ProjectDashboardReadModel(
            String projectId,
            long totalDeliverables,
            long pendingSubmissionCount,
            long pendingReviewCount,
            long approvedCount,
            long rejectedCount,
            long overdueCount,
            long milestonesAtRisk,
            LocalDate nextMilestoneDate) {
    }
}
