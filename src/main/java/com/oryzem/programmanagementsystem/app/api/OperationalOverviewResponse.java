package com.oryzem.programmanagementsystem.app.api;

import java.time.Instant;
import java.util.List;

public record OperationalOverviewResponse(
        OperationalKpisResponse kpis,
        List<OperationalSeriesPointResponse> series,
        List<OperationalTenantSummaryResponse> topTenants,
        List<OperationalTenantDetailResponse> tenantDetails,
        List<OperationalAlertResponse> alerts,
        List<OperationalRecentEventResponse> recentEvents) {

    public record OperationalKpisResponse(
            long totalTenants,
            long activeTenants,
            long totalOrganizations,
            long activeOrganizations,
            long totalUsers,
            long activeUsers,
            long totalMarkets,
            long activeMarkets,
            long totalMemberships,
            long activeMemberships,
            long eventsInWindow,
            long rateLimitResponses,
            long quotaConflicts,
            long offboardEvents,
            long exportRequestedEvents,
            long exportCompletedEvents) {
    }

    public record OperationalSeriesPointResponse(
            String period,
            long events,
            long offboardEvents,
            long exportRequestedEvents,
            long exportCompletedEvents) {
    }

    public record OperationalTenantSummaryResponse(
            String tenantId,
            String tenantName,
            String tenantCode,
            String tenantTier,
            String tenantStatus,
            String tenantType,
            String rootOrganizationId,
            String dataRegion,
            long organizationCount,
            long activeOrganizationCount,
            long marketCount,
            long activeMarketCount,
            long userCount,
            long activeUserCount,
            long membershipCount,
            long activeMembershipCount,
            long eventCount,
            Instant lastActivityAt,
            long activityScore) {
    }

    public record OperationalTenantDetailResponse(
            String tenantId,
            String tenantName,
            String tenantCode,
            String tenantTier,
            String tenantStatus,
            String tenantType,
            String rootOrganizationId,
            String dataRegion,
            long organizationCount,
            long activeOrganizationCount,
            long inactiveOrganizationCount,
            long offboardingOrganizationCount,
            long offboardedOrganizationCount,
            long purgedOrganizationCount,
            long notRequestedExportCount,
            long readyForExportCount,
            long exportInProgressCount,
            long exportedCount,
            long marketCount,
            long activeMarketCount,
            long userCount,
            long activeUserCount,
            long membershipCount,
            long activeMembershipCount,
            long eventCount,
            Instant lastActivityAt,
            long activityScore,
            List<OperationalAlertResponse> alerts,
            List<OperationalRecentEventResponse> recentEvents) {
    }

    public record OperationalAlertResponse(
            String code,
            String severity,
            String message,
            String tenantId,
            String tenantName,
            String path,
            long count,
            Instant observedAt) {
    }

    public record OperationalRecentEventResponse(
            String id,
            String eventType,
            String tenantId,
            String tenantName,
            String targetResourceType,
            String targetResourceId,
            String sourceModule,
            boolean crossTenant,
            String correlationId,
            String summary,
            Instant createdAt) {
    }
}
