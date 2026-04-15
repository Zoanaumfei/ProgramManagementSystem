package com.oryzem.programmanagementsystem.modules.projectmanagement.api;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.DashboardViews;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.model.read.DashboardReadModels;
import java.time.LocalDate;

public final class ProjectDashboardDtos {

    private ProjectDashboardDtos() {
    }

    public record ProjectDashboardResponse(String projectId, long totalDeliverables, long pendingSubmissionCount, long pendingReviewCount, long approvedCount, long rejectedCount, long overdueCount, long milestonesAtRisk, LocalDate nextMilestoneDate) {
        static ProjectDashboardResponse from(DashboardViews.ProjectDashboardView view) {
            return new ProjectDashboardResponse(view.projectId(), view.totalDeliverables(), view.pendingSubmissionCount(), view.pendingReviewCount(), view.approvedCount(), view.rejectedCount(), view.overdueCount(), view.milestonesAtRisk(), view.nextMilestoneDate());
        }

        static ProjectDashboardResponse from(DashboardReadModels.ProjectDashboardReadModel readModel) {
            return new ProjectDashboardResponse(readModel.projectId(), readModel.totalDeliverables(), readModel.pendingSubmissionCount(), readModel.pendingReviewCount(), readModel.approvedCount(), readModel.rejectedCount(), readModel.overdueCount(), readModel.milestonesAtRisk(), readModel.nextMilestoneDate());
        }
    }
}
