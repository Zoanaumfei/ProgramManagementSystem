package com.oryzem.programmanagementsystem.platform.audit;

import com.oryzem.programmanagementsystem.platform.users.deprecation.LegacyUsersFeatureFlagService;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
@Transactional(readOnly = true)
public class AccessAdoptionReportService {

    private final SpringDataAuditLogJpaRepository repository;
    private final LegacyUsersFeatureFlagService featureFlagService;
    private final ObjectMapper objectMapper;

    public AccessAdoptionReportService(
            SpringDataAuditLogJpaRepository repository,
            LegacyUsersFeatureFlagService featureFlagService,
            ObjectMapper objectMapper) {
        this.repository = repository;
        this.featureFlagService = featureFlagService;
        this.objectMapper = objectMapper;
    }

    public AccessAdoptionReportResponse generateReport(int trailingDays) {
        int effectiveTrailingDays = Math.max(7, trailingDays);
        Instant since = Instant.now().minusSeconds(effectiveTrailingDays * 24L * 60L * 60L);
        List<AuditTrailEvent> events = repository.findAllByEventTypeInAndCreatedAtGreaterThanEqualOrderByCreatedAtAscIdAsc(
                        List.of(
                                AccessAdoptionTelemetryService.LEGACY_USERS_API_USAGE_EVENT,
                                AccessAdoptionTelemetryService.MEMBERSHIP_USERS_API_USAGE_EVENT),
                        since)
                .stream()
                .map(AuditLogEntity::toDomain)
                .toList();

        long legacyOperations = events.stream()
                .filter(event -> AccessAdoptionTelemetryService.LEGACY_USERS_API_USAGE_EVENT.equals(event.eventType()))
                .count();
        long membershipOperations = events.stream()
                .filter(event -> AccessAdoptionTelemetryService.MEMBERSHIP_USERS_API_USAGE_EVENT.equals(event.eventType()))
                .count();
        long totalOperations = legacyOperations + membershipOperations;

        List<AccessAdoptionReportResponse.OperationBreakdown> operationBreakdown = buildOperationBreakdown(events);
        List<AccessAdoptionReportResponse.RoleBreakdown> roleBreakdown = buildRoleBreakdown(events);
        List<AccessAdoptionReportResponse.WeeklyTrendPoint> weeklyTrend = buildWeeklyTrend(events);
        List<AccessAdoptionReportResponse.TenantDependency> tenantDependencies = buildTenantDependencies(events);

        return new AccessAdoptionReportResponse(
                effectiveTrailingDays,
                featureFlagService.currentStage(),
                featureFlagService.isUiEnabled(),
                featureFlagService.isReadEnabled(),
                featureFlagService.isWriteEnabled(),
                legacyOperations,
                membershipOperations,
                percentage(legacyOperations, totalOperations),
                percentage(membershipOperations, totalOperations),
                operationBreakdown,
                roleBreakdown,
                weeklyTrend,
                tenantDependencies,
                Instant.now());
    }

    private List<AccessAdoptionReportResponse.OperationBreakdown> buildOperationBreakdown(List<AuditTrailEvent> events) {
        Map<String, Long> counters = new LinkedHashMap<>();
        for (AuditTrailEvent event : events) {
            String apiFamily = apiFamily(event);
            String operation = operation(event);
            String key = apiFamily + "|" + operation;
            counters.merge(key, 1L, Long::sum);
        }
        return counters.entrySet().stream()
                .map(entry -> {
                    String[] parts = entry.getKey().split("\\|", 2);
                    return new AccessAdoptionReportResponse.OperationBreakdown(parts[0], parts[1], entry.getValue());
                })
                .sorted(Comparator.comparing(AccessAdoptionReportResponse.OperationBreakdown::apiFamily)
                        .thenComparing(AccessAdoptionReportResponse.OperationBreakdown::operation))
                .toList();
    }

    private List<AccessAdoptionReportResponse.RoleBreakdown> buildRoleBreakdown(List<AuditTrailEvent> events) {
        Map<String, Long> counters = new LinkedHashMap<>();
        for (AuditTrailEvent event : events) {
            String apiFamily = apiFamily(event);
            String actorRole = event.actorRole().name();
            String key = apiFamily + "|" + actorRole;
            counters.merge(key, 1L, Long::sum);
        }
        return counters.entrySet().stream()
                .map(entry -> {
                    String[] parts = entry.getKey().split("\\|", 2);
                    return new AccessAdoptionReportResponse.RoleBreakdown(parts[0], parts[1], entry.getValue());
                })
                .sorted(Comparator.comparing(AccessAdoptionReportResponse.RoleBreakdown::apiFamily)
                        .thenComparing(AccessAdoptionReportResponse.RoleBreakdown::actorRole))
                .toList();
    }

    private List<AccessAdoptionReportResponse.WeeklyTrendPoint> buildWeeklyTrend(List<AuditTrailEvent> events) {
        class WeeklyAccumulator {
            long legacyOperations;
            long membershipOperations;
        }

        Map<LocalDate, WeeklyAccumulator> byWeek = new LinkedHashMap<>();
        for (AuditTrailEvent event : events) {
            LocalDate weekStart = event.createdAt()
                    .atZone(ZoneOffset.UTC)
                    .toLocalDate()
                    .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            WeeklyAccumulator accumulator = byWeek.computeIfAbsent(weekStart, ignored -> new WeeklyAccumulator());
            if (AccessAdoptionTelemetryService.LEGACY_USERS_API_USAGE_EVENT.equals(event.eventType())) {
                accumulator.legacyOperations++;
            } else if (AccessAdoptionTelemetryService.MEMBERSHIP_USERS_API_USAGE_EVENT.equals(event.eventType())) {
                accumulator.membershipOperations++;
            }
        }

        List<AccessAdoptionReportResponse.WeeklyTrendPoint> trend = new ArrayList<>();
        byWeek.forEach((weekStart, accumulator) -> {
            long total = accumulator.legacyOperations + accumulator.membershipOperations;
            trend.add(new AccessAdoptionReportResponse.WeeklyTrendPoint(
                    weekStart,
                    accumulator.legacyOperations,
                    accumulator.membershipOperations,
                    percentage(accumulator.legacyOperations, total)));
        });
        return trend;
    }

    private List<AccessAdoptionReportResponse.TenantDependency> buildTenantDependencies(List<AuditTrailEvent> events) {
        class TenantAccumulator {
            long legacyOperations;
            long membershipOperations;
            Instant lastLegacyUsageAt;
            Set<String> roles = new LinkedHashSet<>();
            Set<String> operations = new LinkedHashSet<>();
        }

        Map<String, TenantAccumulator> byTenant = new LinkedHashMap<>();
        for (AuditTrailEvent event : events) {
            String tenantId = hasText(event.targetTenantId()) ? event.targetTenantId() : fallbackTenantId(event);
            TenantAccumulator accumulator = byTenant.computeIfAbsent(tenantId, ignored -> new TenantAccumulator());
            accumulator.roles.add(event.actorRole().name());
            accumulator.operations.add(operation(event));
            if (AccessAdoptionTelemetryService.LEGACY_USERS_API_USAGE_EVENT.equals(event.eventType())) {
                accumulator.legacyOperations++;
                accumulator.lastLegacyUsageAt = event.createdAt();
            } else if (AccessAdoptionTelemetryService.MEMBERSHIP_USERS_API_USAGE_EVENT.equals(event.eventType())) {
                accumulator.membershipOperations++;
            }
        }

        return byTenant.entrySet().stream()
                .filter(entry -> entry.getValue().legacyOperations > 0)
                .map(entry -> {
                    TenantAccumulator accumulator = entry.getValue();
                    long total = accumulator.legacyOperations + accumulator.membershipOperations;
                    return new AccessAdoptionReportResponse.TenantDependency(
                            entry.getKey(),
                            accumulator.legacyOperations,
                            accumulator.membershipOperations,
                            percentage(accumulator.legacyOperations, total),
                            accumulator.lastLegacyUsageAt,
                            accumulator.roles.stream().sorted().toList(),
                            accumulator.operations.stream().sorted().toList());
                })
                .sorted(Comparator.comparingLong(AccessAdoptionReportResponse.TenantDependency::legacyOperations).reversed()
                        .thenComparing(AccessAdoptionReportResponse.TenantDependency::tenantId))
                .toList();
    }

    private String apiFamily(AuditTrailEvent event) {
        if (AccessAdoptionTelemetryService.LEGACY_USERS_API_USAGE_EVENT.equals(event.eventType())) {
            return "legacy_users";
        }
        if (AccessAdoptionTelemetryService.MEMBERSHIP_USERS_API_USAGE_EVENT.equals(event.eventType())) {
            return "membership_users";
        }
        return "unknown";
    }

    private String operation(AuditTrailEvent event) {
        if (!hasText(event.metadataJson())) {
            return "unknown";
        }
        try {
            JsonNode metadata = objectMapper.readTree(event.metadataJson());
            JsonNode operationNode = metadata.get("operation");
            if (operationNode != null && operationNode.isTextual() && !operationNode.asText().isBlank()) {
                return operationNode.asText().trim().toLowerCase(Locale.ROOT);
            }
        } catch (Exception ignored) {
            // Keep the report resilient even if one metadata payload is malformed.
        }
        return "unknown";
    }

    private String fallbackTenantId(AuditTrailEvent event) {
        if (hasText(event.actorTenantId())) {
            return event.actorTenantId();
        }
        return "unknown";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private double percentage(long value, long total) {
        if (total <= 0) {
            return 0.0d;
        }
        return Math.round((value * 10000.0d) / total) / 100.0d;
    }
}
