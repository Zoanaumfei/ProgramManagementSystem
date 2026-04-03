package com.oryzem.programmanagementsystem.app.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class OperationalMetricsService {

    private final MeterRegistry meterRegistry;

    public OperationalMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordTenantRateLimitExceeded(String tenantId, String tenantTier, String path) {
        counter("oryzem.operational.http.responses", tenantId, tenantTier, path, "429", "rate_limit").increment();
    }

    public void recordQuotaConflict(String tenantId, String tenantTier, String quotaType) {
        Counter.builder("oryzem.operational.http.responses")
                .description("Operational HTTP responses grouped by tenant and category.")
                .tag("tenantId", tagValue(tenantId))
                .tag("tenantTier", tagValue(tenantTier))
                .tag("path", "quota")
                .tag("status", "409")
                .tag("category", "quota_" + tagValue(quotaType))
                .register(meterRegistry)
                .increment();
    }

    public void recordOrganizationOffboard(String tenantId) {
        operationalEventCounter("organization_offboard", tenantId).increment();
    }

    public void recordOrganizationExportRequested(String tenantId) {
        operationalEventCounter("organization_export_requested", tenantId).increment();
    }

    public void recordOrganizationExportCompleted(String tenantId) {
        operationalEventCounter("organization_export_completed", tenantId).increment();
    }

    private Counter operationalEventCounter(String eventType, String tenantId) {
        return Counter.builder("oryzem.operational.events")
                .description("Operational tenant events used by dashboards and alerts.")
                .tag("eventType", tagValue(eventType))
                .tag("tenantId", tagValue(tenantId))
                .register(meterRegistry);
    }

    private Counter counter(
            String meterName,
            String tenantId,
            String tenantTier,
            String path,
            String status,
            String category) {
        return Counter.builder(meterName)
                .description("Operational HTTP responses grouped by tenant and category.")
                .tag("tenantId", tagValue(tenantId))
                .tag("tenantTier", tagValue(tenantTier))
                .tag("path", tagValue(path))
                .tag("status", tagValue(status))
                .tag("category", tagValue(category))
                .register(meterRegistry);
    }

    private String tagValue(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.trim();
    }
}
