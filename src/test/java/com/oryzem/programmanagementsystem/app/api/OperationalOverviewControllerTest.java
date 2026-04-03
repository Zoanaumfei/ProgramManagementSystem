package com.oryzem.programmanagementsystem.app.api;

import com.oryzem.programmanagementsystem.app.bootstrap.BootstrapDataService;
import com.oryzem.programmanagementsystem.app.monitoring.OperationalMetricsService;
import com.oryzem.programmanagementsystem.platform.audit.AuditTrailEvent;
import com.oryzem.programmanagementsystem.platform.audit.AuditTrailService;
import com.oryzem.programmanagementsystem.platform.authorization.Role;
import java.time.Instant;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = com.oryzem.programmanagementsystem.app.ProgramManagementSystemApplication.class)
@AutoConfigureMockMvc
class OperationalOverviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BootstrapDataService bootstrapDataService;

    @Autowired
    private AuditTrailService auditTrailService;

    @Autowired
    private OperationalMetricsService operationalMetricsService;

    @BeforeEach
    void setUp() {
        bootstrapDataService.reset();
        seedOperationalSignals();
    }

    @Test
    void supportShouldReadTenantOverviewWithAggregates() throws Exception {
        mockMvc.perform(get("/api/admin/operational/overview")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPPORT")))
                        .param("tenantId", "TEN-tenant-a")
                        .param("from", "2026-04-01")
                        .param("to", "2026-04-03")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kpis.totalTenants").value(Matchers.greaterThan(0)))
                .andExpect(jsonPath("$.kpis.rateLimitResponses").value(Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.kpis.quotaConflicts").value(Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.series").isArray())
                .andExpect(jsonPath("$.topTenants").isArray())
                .andExpect(jsonPath("$.tenantDetails").isArray())
                .andExpect(jsonPath("$.alerts").isArray())
                .andExpect(jsonPath("$.recentEvents").isArray())
                .andExpect(jsonPath("$.alerts[*].code", Matchers.hasItem("RATE_LIMIT_EXCEEDED")))
                .andExpect(jsonPath("$.alerts[*].code", Matchers.hasItem("QUOTA_CONFLICT")))
                .andExpect(jsonPath("$.recentEvents[*].eventType", Matchers.hasItem("organization_offboard")))
                .andExpect(jsonPath("$.tenantDetails[0].alerts").isArray())
                .andExpect(jsonPath("$.tenantDetails[0].recentEvents").isArray());
    }

    @Test
    void auditorShouldReadRepeatedTenantFilters() throws Exception {
        mockMvc.perform(get("/api/admin/operational/overview")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_AUDITOR")))
                        .param("tenantId", "TEN-tenant-a")
                        .param("tenantId", "TEN-tenant-b")
                        .param("from", "2026-04-01")
                        .param("to", "2026-04-03")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantDetails", Matchers.hasSize(Matchers.greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.topTenants").isArray());
    }

    @Test
    void managerShouldNotReadOperationalOverview() throws Exception {
        mockMvc.perform(get("/api/admin/operational/overview")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_MANAGER")))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    private void seedOperationalSignals() {
        Instant first = Instant.parse("2026-04-01T10:00:00Z");
        Instant second = Instant.parse("2026-04-02T10:00:00Z");
        Instant third = Instant.parse("2026-04-02T12:00:00Z");

        auditTrailService.record(new AuditTrailEvent(
                null,
                "organization_offboard",
                "USR-EXT-A-ADM-001",
                Role.ADMIN,
                "TEN-tenant-a",
                "TEN-tenant-a",
                "ORGANIZATION",
                "ORG-TENANT-A-001",
                null,
                "{\"path\":\"/api/admin/operational/overview\"}",
                false,
                null,
                "test",
                first));
        auditTrailService.record(new AuditTrailEvent(
                null,
                "organization_export_requested",
                "USR-EXT-A-ADM-001",
                Role.ADMIN,
                "TEN-tenant-a",
                "TEN-tenant-a",
                "ORGANIZATION",
                "ORG-TENANT-A-001",
                null,
                "{\"path\":\"/api/admin/operational/overview\"}",
                false,
                null,
                "test",
                second));
        auditTrailService.record(new AuditTrailEvent(
                null,
                "organization_export_completed",
                "USR-EXT-B-ADM-001",
                Role.ADMIN,
                "TEN-tenant-b",
                "TEN-tenant-b",
                "ORGANIZATION",
                "ORG-TENANT-B-001",
                null,
                "{\"path\":\"/api/admin/operational/overview\"}",
                false,
                null,
                "test",
                third));

        operationalMetricsService.recordTenantRateLimitExceeded("TEN-tenant-a", "STANDARD", "/api/admin/operational/overview");
        operationalMetricsService.recordQuotaConflict("TEN-tenant-a", "STANDARD", "organizations");
    }
}
