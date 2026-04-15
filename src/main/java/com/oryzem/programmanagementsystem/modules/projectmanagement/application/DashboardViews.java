package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import java.time.LocalDate;

public final class DashboardViews {

    private DashboardViews() {
    }

    public record ProjectDashboardView(
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
