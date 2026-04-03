package com.oryzem.programmanagementsystem.platform.access;

import com.oryzem.programmanagementsystem.app.api.OperationalOverviewResponse;
import com.oryzem.programmanagementsystem.app.api.OperationalOverviewResponse.OperationalAlertResponse;
import com.oryzem.programmanagementsystem.app.api.OperationalOverviewResponse.OperationalKpisResponse;
import com.oryzem.programmanagementsystem.app.api.OperationalOverviewResponse.OperationalRecentEventResponse;
import com.oryzem.programmanagementsystem.app.api.OperationalOverviewResponse.OperationalSeriesPointResponse;
import com.oryzem.programmanagementsystem.app.api.OperationalOverviewResponse.OperationalTenantDetailResponse;
import com.oryzem.programmanagementsystem.app.api.OperationalOverviewResponse.OperationalTenantSummaryResponse;
import com.oryzem.programmanagementsystem.platform.audit.AuditTrailEvent;
import com.oryzem.programmanagementsystem.platform.audit.AuditTrailService;
import com.oryzem.programmanagementsystem.platform.access.TenantServiceTier;
import com.oryzem.programmanagementsystem.platform.tenant.OrganizationOperationalSnapshot;
import com.oryzem.programmanagementsystem.platform.tenant.OrganizationOperationalSnapshotService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class OperationalOverviewService {

    private static final String OPERATIONAL_HTTP_RESPONSES_METRIC = "oryzem.operational.http.responses";
    private static final int DEFAULT_TENANT_LIMIT = 10;
    private static final int DEFAULT_RECENT_EVENT_LIMIT = 20;
    private static final int DEFAULT_TENANT_EVENT_LIMIT = 3;

    private final TenantOperationalSnapshotService tenantSnapshotService;
    private final OrganizationOperationalSnapshotService organizationSnapshotService;
    private final AuditTrailService auditTrailService;
    private final MeterRegistry meterRegistry;

    public OperationalOverviewService(
            TenantOperationalSnapshotService tenantSnapshotService,
            OrganizationOperationalSnapshotService organizationSnapshotService,
            AuditTrailService auditTrailService,
            MeterRegistry meterRegistry) {
        this.tenantSnapshotService = tenantSnapshotService;
        this.organizationSnapshotService = organizationSnapshotService;
        this.auditTrailService = auditTrailService;
        this.meterRegistry = meterRegistry;
    }

    public OperationalOverviewResponse getOverview(
            List<String> tenantIds,
            String tenantTier,
            String path,
            Boolean activeOnly,
            String from,
            String to) {
        Instant rangeEnd = parseInstantOrDate(to, true);
        if (rangeEnd == null) {
            rangeEnd = Instant.now();
        }
        Instant rangeStart = parseInstantOrDate(from, false);
        if (rangeStart == null) {
            rangeStart = LocalDate.now(ZoneOffset.UTC).minusDays(29).atStartOfDay().toInstant(ZoneOffset.UTC);
        }
        if (rangeStart.isAfter(rangeEnd)) {
            throw new IllegalArgumentException("The 'from' filter must be before or equal to 'to'.");
        }

        final Instant effectiveRangeStart = rangeStart;
        final Instant effectiveRangeEnd = rangeEnd;

        Set<String> normalizedTenantIds = normalizeTenantIds(tenantIds);
        TenantServiceTier tenantTierFilter = parseTenantTier(tenantTier);
        boolean onlyActive = Boolean.TRUE.equals(activeOnly);

        List<TenantOperationalSnapshot> tenantSnapshots = tenantSnapshotService.findAllSnapshots();
        Map<String, OrganizationOperationalSnapshot> organizationSnapshots = organizationSnapshotService.findAllSnapshots().stream()
                .collect(Collectors.toMap(
                        OrganizationOperationalSnapshot::tenantId,
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new));
        List<AuditTrailEvent> allEvents = auditTrailService.findAll();

        List<TenantView> tenantViews = tenantSnapshots.stream()
                .filter(snapshot -> normalizedTenantIds.isEmpty() || normalizedTenantIds.contains(snapshot.tenantId()))
                .filter(snapshot -> tenantTierFilter == null || tenantTierFilter.name().equalsIgnoreCase(snapshot.tenantTier()))
                .filter(snapshot -> !onlyActive || "ACTIVE".equalsIgnoreCase(snapshot.tenantStatus()))
                .map(snapshot -> toTenantView(snapshot, organizationSnapshots.get(snapshot.tenantId()), allEvents, effectiveRangeStart, effectiveRangeEnd))
                .sorted(Comparator.comparingLong(TenantView::activityScore).reversed()
                        .thenComparing(view -> view.snapshot().tenantName(), String.CASE_INSENSITIVE_ORDER))
                .toList();

        Set<String> activeTenantIds = tenantViews.stream()
                .map(view -> view.snapshot().tenantId())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> allowedTenantIdsForEvents = normalizedTenantIds.isEmpty() && tenantTierFilter == null && !onlyActive
                ? null
                : activeTenantIds;

        List<AuditTrailEvent> filteredEvents = filterEvents(allEvents, effectiveRangeStart, effectiveRangeEnd, allowedTenantIdsForEvents);

        List<OperationalSeriesPointResponse> series = buildSeries(filteredEvents, effectiveRangeStart, effectiveRangeEnd, allowedTenantIdsForEvents);
        List<OperationalRecentEventResponse> recentEvents = buildRecentEvents(filteredEvents, tenantViews, DEFAULT_RECENT_EVENT_LIMIT);
        List<OperationalAlertResponse> alerts = buildAlerts(tenantViews, recentEvents, normalizedTenantIds, tenantTierFilter, path, onlyActive);

        OperationalKpisResponse kpis = buildKpis(filteredEvents, tenantViews, path);

        List<TenantView> detailViews = normalizedTenantIds.isEmpty()
                ? tenantViews.stream().limit(DEFAULT_TENANT_LIMIT).toList()
                : tenantViews;

        List<OperationalTenantSummaryResponse> topTenants = tenantViews.stream()
                .limit(DEFAULT_TENANT_LIMIT)
                .map(this::toSummaryResponse)
                .toList();
        List<OperationalTenantDetailResponse> tenantDetails = detailViews.stream()
                .map(view -> toDetailResponse(view, alerts, recentEvents))
                .toList();

        return new OperationalOverviewResponse(
                kpis,
                series,
                topTenants,
                tenantDetails,
                alerts,
                recentEvents);
    }

    private OperationalKpisResponse buildKpis(List<AuditTrailEvent> filteredEvents, List<TenantView> tenantViews, String path) {
        Set<String> tenantIds = tenantViews.stream()
                .map(view -> view.snapshot().tenantId())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<OperationalMetricSnapshot> metricSnapshots = readMetricSnapshots(path, tenantIds);

        long totalTenants = tenantViews.size();
        long activeTenants = tenantViews.stream()
                .filter(view -> "ACTIVE".equalsIgnoreCase(view.snapshot().tenantStatus()))
                .count();
        long totalOrganizations = tenantViews.stream().mapToLong(view -> view.organizationSnapshot().organizationCount()).sum();
        long activeOrganizations = tenantViews.stream().mapToLong(view -> view.organizationSnapshot().activeOrganizationCount()).sum();
        long totalUsers = tenantViews.stream().mapToLong(view -> view.snapshot().userCount()).sum();
        long activeUsers = tenantViews.stream().mapToLong(view -> view.snapshot().activeUserCount()).sum();
        long totalMarkets = tenantViews.stream().mapToLong(view -> view.snapshot().marketCount()).sum();
        long activeMarkets = tenantViews.stream().mapToLong(view -> view.snapshot().activeMarketCount()).sum();
        long totalMemberships = tenantViews.stream().mapToLong(view -> view.snapshot().membershipCount()).sum();
        long activeMemberships = tenantViews.stream().mapToLong(view -> view.snapshot().activeMembershipCount()).sum();
        long eventsInWindow = filteredEvents.size();
        long rateLimitResponses = (long) metricSnapshots.stream()
                .filter(snapshot -> "429".equals(snapshot.status()))
                .mapToDouble(OperationalMetricSnapshot::count)
                .sum();
        long quotaConflicts = (long) metricSnapshots.stream()
                .filter(snapshot -> "409".equals(snapshot.status()))
                .mapToDouble(OperationalMetricSnapshot::count)
                .sum();
        long offboardEvents = countEventType(filteredEvents, "organization_offboard");
        long exportRequestedEvents = countEventType(filteredEvents, "organization_export_requested");
        long exportCompletedEvents = countEventType(filteredEvents, "organization_export_completed");

        return new OperationalKpisResponse(
                totalTenants,
                activeTenants,
                totalOrganizations,
                activeOrganizations,
                totalUsers,
                activeUsers,
                totalMarkets,
                activeMarkets,
                totalMemberships,
                activeMemberships,
                eventsInWindow,
                rateLimitResponses,
                quotaConflicts,
                offboardEvents,
                exportRequestedEvents,
                exportCompletedEvents);
    }

    private List<OperationalSeriesPointResponse> buildSeries(
            List<AuditTrailEvent> filteredEvents,
            Instant rangeStart,
            Instant rangeEnd,
            Set<String> allowedTenantIds) {
        Map<LocalDate, SeriesAccumulator> accumulators = new LinkedHashMap<>();
        LocalDate startDate = rangeStart.atZone(ZoneOffset.UTC).toLocalDate();
        LocalDate endDate = rangeEnd.atZone(ZoneOffset.UTC).toLocalDate();
        for (LocalDate current = startDate; !current.isAfter(endDate); current = current.plusDays(1)) {
            accumulators.put(current, new SeriesAccumulator());
        }

        for (AuditTrailEvent event : filteredEvents) {
            LocalDate bucket = event.createdAt().atZone(ZoneOffset.UTC).toLocalDate();
            SeriesAccumulator accumulator = accumulators.computeIfAbsent(bucket, ignored -> new SeriesAccumulator());
            if (matchesTenant(event, allowedTenantIds)) {
                accumulators.put(bucket, accumulator.increment(event.eventType()));
            }
        }

        return accumulators.entrySet().stream()
                .map(entry -> new OperationalSeriesPointResponse(
                        entry.getKey().toString(),
                        entry.getValue().events(),
                        entry.getValue().offboardEvents(),
                        entry.getValue().exportRequestedEvents(),
                        entry.getValue().exportCompletedEvents()))
                .toList();
    }

    private List<OperationalRecentEventResponse> buildRecentEvents(
            List<AuditTrailEvent> filteredEvents,
            List<TenantView> tenantViews,
            int limit) {
        Map<String, TenantView> tenantViewsById = tenantViews.stream()
                .collect(Collectors.toMap(view -> view.snapshot().tenantId(), Function.identity(), (left, right) -> left, LinkedHashMap::new));

        return filteredEvents.stream()
                .sorted(Comparator.comparing(AuditTrailEvent::createdAt).reversed())
                .limit(limit)
                .map(event -> toRecentEventResponse(event, tenantViewsById))
                .toList();
    }

    private List<OperationalAlertResponse> buildAlerts(
            List<TenantView> tenantViews,
            List<OperationalRecentEventResponse> recentEvents,
            Set<String> normalizedTenantIds,
            TenantServiceTier tenantTierFilter,
            String path,
            boolean onlyActive) {
        List<OperationalAlertResponse> alerts = new ArrayList<>();
        Set<String> allowedTenantIds = tenantViews.stream()
                .map(view -> view.snapshot().tenantId())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<OperationalMetricSnapshot> metricSnapshots = readMetricSnapshots(path, allowedTenantIds);

        metricSnapshots.stream()
                .filter(snapshot -> "429".equals(snapshot.status()) || "409".equals(snapshot.status()))
                .map(snapshot -> toMetricAlert(snapshot, tenantViews))
                .forEach(alerts::add);

        tenantViews.stream()
                .filter(view -> view.organizationSnapshot().offboardingOrganizationCount() > 0)
                .forEach(view -> alerts.add(new OperationalAlertResponse(
                        "OFFBOARDING_IN_PROGRESS",
                        "info",
                        "Tenant has organizations currently offboarding.",
                        view.snapshot().tenantId(),
                        view.snapshot().tenantName(),
                        null,
                        view.organizationSnapshot().offboardingOrganizationCount(),
                        recentEvents.stream()
                                .filter(event -> view.snapshot().tenantId().equals(event.tenantId()))
                                .map(OperationalRecentEventResponse::createdAt)
                                .findFirst()
                                .orElse(Instant.now()))));

        tenantViews.stream()
                .filter(view -> view.organizationSnapshot().offboardedOrganizationCount() > view.organizationSnapshot().exportedCount())
                .forEach(view -> alerts.add(new OperationalAlertResponse(
                        "EXPORT_BACKLOG",
                        "warning",
                        "Offboarded organizations are waiting for export completion.",
                        view.snapshot().tenantId(),
                        view.snapshot().tenantName(),
                        null,
                        view.organizationSnapshot().offboardedOrganizationCount() - view.organizationSnapshot().exportedCount(),
                        Instant.now())));

        if (onlyActive && tenantViews.isEmpty() && !normalizedTenantIds.isEmpty()) {
            alerts.add(new OperationalAlertResponse(
                    "NO_ACTIVE_TENANTS",
                    "warning",
                    "No active tenants matched the requested filters.",
                    null,
                    null,
                    null,
                    0,
                    Instant.now()));
        } else if (tenantTierFilter != null && tenantViews.isEmpty()) {
            alerts.add(new OperationalAlertResponse(
                    "NO_TENANTS_FOR_TIER",
                    "warning",
                    "No tenants matched the requested service tier.",
                    null,
                    null,
                    null,
                    0,
                    Instant.now()));
        }

        return alerts;
    }

    private OperationalTenantSummaryResponse toSummaryResponse(TenantView view) {
        return new OperationalTenantSummaryResponse(
                view.snapshot().tenantId(),
                view.snapshot().tenantName(),
                view.snapshot().tenantCode(),
                view.snapshot().tenantTier(),
                view.snapshot().tenantStatus(),
                view.snapshot().tenantType(),
                view.snapshot().rootOrganizationId(),
                view.snapshot().dataRegion(),
                view.organizationSnapshot().organizationCount(),
                view.organizationSnapshot().activeOrganizationCount(),
                view.snapshot().marketCount(),
                view.snapshot().activeMarketCount(),
                view.snapshot().userCount(),
                view.snapshot().activeUserCount(),
                view.snapshot().membershipCount(),
                view.snapshot().activeMembershipCount(),
                view.eventCount(),
                view.lastActivityAt(),
                view.activityScore());
    }

    private OperationalTenantDetailResponse toDetailResponse(
            TenantView view,
            List<OperationalAlertResponse> alerts,
            List<OperationalRecentEventResponse> recentEvents) {
        List<OperationalAlertResponse> tenantAlerts = alerts.stream()
                .filter(alert -> view.snapshot().tenantId().equals(alert.tenantId()))
                .limit(DEFAULT_TENANT_EVENT_LIMIT)
                .toList();
        List<OperationalRecentEventResponse> tenantRecentEvents = recentEvents.stream()
                .filter(event -> view.snapshot().tenantId().equals(event.tenantId()))
                .limit(DEFAULT_TENANT_EVENT_LIMIT)
                .toList();

        return new OperationalTenantDetailResponse(
                view.snapshot().tenantId(),
                view.snapshot().tenantName(),
                view.snapshot().tenantCode(),
                view.snapshot().tenantTier(),
                view.snapshot().tenantStatus(),
                view.snapshot().tenantType(),
                view.snapshot().rootOrganizationId(),
                view.snapshot().dataRegion(),
                view.organizationSnapshot().organizationCount(),
                view.organizationSnapshot().activeOrganizationCount(),
                view.organizationSnapshot().inactiveOrganizationCount(),
                view.organizationSnapshot().offboardingOrganizationCount(),
                view.organizationSnapshot().offboardedOrganizationCount(),
                view.organizationSnapshot().purgedOrganizationCount(),
                view.organizationSnapshot().notRequestedExportCount(),
                view.organizationSnapshot().readyForExportCount(),
                view.organizationSnapshot().exportInProgressCount(),
                view.organizationSnapshot().exportedCount(),
                view.snapshot().marketCount(),
                view.snapshot().activeMarketCount(),
                view.snapshot().userCount(),
                view.snapshot().activeUserCount(),
                view.snapshot().membershipCount(),
                view.snapshot().activeMembershipCount(),
                view.eventCount(),
                view.lastActivityAt(),
                view.activityScore(),
                tenantAlerts,
                tenantRecentEvents);
    }

    private TenantView toTenantView(
            TenantOperationalSnapshot snapshot,
            OrganizationOperationalSnapshot organizationSnapshot,
            List<AuditTrailEvent> allEvents,
            Instant rangeStart,
            Instant rangeEnd) {
        OrganizationOperationalSnapshot resolvedOrganizationSnapshot = organizationSnapshot != null
                ? organizationSnapshot
                : new OrganizationOperationalSnapshot(
                        snapshot.tenantId(),
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0);
        List<AuditTrailEvent> tenantEvents = allEvents.stream()
                .filter(event -> matchesTenant(event, Set.of(snapshot.tenantId())))
                .filter(event -> !event.createdAt().isBefore(rangeStart) && !event.createdAt().isAfter(rangeEnd))
                .toList();
        long eventCount = tenantEvents.size();
        Instant lastActivityAt = tenantEvents.stream()
                .map(AuditTrailEvent::createdAt)
                .max(Comparator.naturalOrder())
                .orElse(null);
        long activityScore = calculateActivityScore(snapshot, resolvedOrganizationSnapshot, eventCount);
        return new TenantView(snapshot, resolvedOrganizationSnapshot, tenantEvents, eventCount, lastActivityAt, activityScore);
    }

    private long calculateActivityScore(
            TenantOperationalSnapshot snapshot,
            OrganizationOperationalSnapshot organizationSnapshot,
            long eventCount) {
        return (organizationSnapshot.organizationCount() * 5L)
                + (snapshot.marketCount() * 3L)
                + (snapshot.userCount() * 3L)
                + (snapshot.membershipCount() * 2L)
                + eventCount;
    }

    private List<AuditTrailEvent> filterEvents(List<AuditTrailEvent> events, Instant start, Instant end, Set<String> allowedTenantIds) {
        return events.stream()
                .filter(event -> !event.createdAt().isBefore(start) && !event.createdAt().isAfter(end))
                .filter(event -> allowedTenantIds == null || allowedTenantIds.isEmpty() || matchesTenant(event, allowedTenantIds))
                .toList();
    }

    private boolean matchesTenant(AuditTrailEvent event, Set<String> allowedTenantIds) {
        if (allowedTenantIds == null) {
            return true;
        }
        if (allowedTenantIds.isEmpty()) {
            return false;
        }
        return allowedTenantIds.contains(event.actorTenantId()) || allowedTenantIds.contains(event.targetTenantId());
    }

    private OperationalRecentEventResponse toRecentEventResponse(
            AuditTrailEvent event,
            Map<String, TenantView> tenantViewsById) {
        String tenantId = resolveTenantId(event, tenantViewsById.keySet());
        String tenantName = tenantId == null || tenantViewsById.get(tenantId) == null
                ? null
                : tenantViewsById.get(tenantId).snapshot().tenantName();
        return new OperationalRecentEventResponse(
                event.id(),
                event.eventType(),
                tenantId,
                tenantName,
                event.targetResourceType(),
                event.targetResourceId(),
                event.sourceModule(),
                event.crossTenant(),
                event.correlationId(),
                buildSummary(event, tenantName),
                event.createdAt());
    }

    private String resolveTenantId(AuditTrailEvent event, Set<String> knownTenantIds) {
        if (knownTenantIds.contains(event.targetTenantId())) {
            return event.targetTenantId();
        }
        if (knownTenantIds.contains(event.actorTenantId())) {
            return event.actorTenantId();
        }
        return event.targetTenantId() != null ? event.targetTenantId() : event.actorTenantId();
    }

    private String buildSummary(AuditTrailEvent event, String tenantName) {
        String tenantLabel = tenantName == null ? event.targetTenantId() : tenantName;
        return switch (event.eventType()) {
            case "organization_offboard" -> "Organization offboarding requested for " + tenantLabel + ".";
            case "organization_export_requested" -> "Organization export requested for " + tenantLabel + ".";
            case "organization_export_completed" -> "Organization export completed for " + tenantLabel + ".";
            default -> event.eventType() + " recorded for " + tenantLabel + ".";
        };
    }

    private OperationalAlertResponse toMetricAlert(OperationalMetricSnapshot snapshot, List<TenantView> tenantViews) {
        String tenantName = tenantViews.stream()
                .filter(view -> view.snapshot().tenantId().equals(snapshot.tenantId()))
                .map(view -> view.snapshot().tenantName())
                .findFirst()
                .orElse(snapshot.tenantId());
        String code = "429".equals(snapshot.status()) ? "RATE_LIMIT_EXCEEDED" : "QUOTA_CONFLICT";
        String severity = "429".equals(snapshot.status()) ? "warning" : "critical";
        String message = "429".equals(snapshot.status())
                ? "Rate limit responses detected on this path."
                : "Quota conflicts detected on this path.";
        return new OperationalAlertResponse(
                code,
                severity,
                message,
                snapshot.tenantId(),
                tenantName,
                snapshot.path(),
                Math.round(snapshot.count()),
                Instant.now());
    }

    private long countEventType(List<AuditTrailEvent> filteredEvents, String eventType) {
        return filteredEvents.stream()
                .filter(event -> eventType.equals(event.eventType()))
                .count();
    }

    private List<OperationalMetricSnapshot> readMetricSnapshots(String path, Set<String> allowedTenantIds) {
        return meterRegistry.getMeters().stream()
                .filter(meter -> OPERATIONAL_HTTP_RESPONSES_METRIC.equals(meter.getId().getName()))
                .map(this::toMetricSnapshot)
                .filter(snapshot -> path == null || path.isBlank() || path.equals(snapshot.path()))
                .filter(snapshot -> allowedTenantIds == null || allowedTenantIds.contains(snapshot.tenantId()))
                .toList();
    }

    private OperationalMetricSnapshot toMetricSnapshot(Meter meter) {
        double count = meter instanceof Counter counter ? counter.count() : 0.0d;
        return new OperationalMetricSnapshot(
                meter.getId().getTag("tenantId"),
                meter.getId().getTag("tenantTier"),
                meter.getId().getTag("path"),
                meter.getId().getTag("status"),
                meter.getId().getTag("category"),
                count);
    }

    private Set<String> normalizeTenantIds(List<String> tenantIds) {
        if (tenantIds == null || tenantIds.isEmpty()) {
            return Set.of();
        }
        return tenantIds.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private TenantServiceTier parseTenantTier(String tenantTier) {
        if (tenantTier == null || tenantTier.isBlank()) {
            return null;
        }
        try {
            return TenantServiceTier.valueOf(tenantTier.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unknown tenantTier value: " + tenantTier);
        }
    }

    private Instant parseInstantOrDate(String value, boolean endOfRange) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        try {
            return Instant.parse(trimmed);
        } catch (DateTimeParseException ignored) {
            // Fall through to date parsing.
        }
        try {
            LocalDate date = LocalDate.parse(trimmed);
            return endOfRange
                    ? date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).minusNanos(1)
                    : date.atStartOfDay().toInstant(ZoneOffset.UTC);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Invalid date/time value: " + value);
        }
    }

    private record TenantView(
            TenantOperationalSnapshot snapshot,
            OrganizationOperationalSnapshot organizationSnapshot,
            List<AuditTrailEvent> events,
            long eventCount,
            Instant lastActivityAt,
            long activityScore) {
    }

    private record SeriesAccumulator(long events, long offboardEvents, long exportRequestedEvents, long exportCompletedEvents) {
        SeriesAccumulator() {
            this(0, 0, 0, 0);
        }

        SeriesAccumulator increment(String eventType) {
            long nextEvents = events + 1;
            long nextOffboardEvents = offboardEvents + ("organization_offboard".equals(eventType) ? 1 : 0);
            long nextExportRequestedEvents = exportRequestedEvents + ("organization_export_requested".equals(eventType) ? 1 : 0);
            long nextExportCompletedEvents = exportCompletedEvents + ("organization_export_completed".equals(eventType) ? 1 : 0);
            return new SeriesAccumulator(nextEvents, nextOffboardEvents, nextExportRequestedEvents, nextExportCompletedEvents);
        }
    }

    private record OperationalMetricSnapshot(
            String tenantId,
            String tenantTier,
            String path,
            String status,
            String category,
            double count) {
    }
}
