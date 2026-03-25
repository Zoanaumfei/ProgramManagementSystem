package com.oryzem.programmanagementsystem.platform.audit;

import com.oryzem.programmanagementsystem.platform.users.deprecation.LegacyUsersDeprecationStage;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record AccessAdoptionReportResponse(
        int trailingDays,
        LegacyUsersDeprecationStage currentStage,
        boolean usersLegacyUiEnabled,
        boolean usersLegacyReadEnabled,
        boolean usersLegacyWriteEnabled,
        long legacyOperations,
        long membershipOperations,
        double legacySharePercent,
        double membershipSharePercent,
        List<OperationBreakdown> operationBreakdown,
        List<RoleBreakdown> roleBreakdown,
        List<WeeklyTrendPoint> weeklyTrend,
        List<TenantDependency> tenantsStillDependentOnLegacy,
        Instant generatedAt) {

    public record OperationBreakdown(
            String apiFamily,
            String operation,
            long count) {
    }

    public record WeeklyTrendPoint(
            LocalDate weekStart,
            long legacyOperations,
            long membershipOperations,
            double legacySharePercent) {
    }

    public record RoleBreakdown(
            String apiFamily,
            String actorRole,
            long count) {
    }

    public record TenantDependency(
            String tenantId,
            long legacyOperations,
            long membershipOperations,
            double legacySharePercent,
            Instant lastLegacyUsageAt,
            List<String> actorRoles,
            List<String> operations) {
    }
}
