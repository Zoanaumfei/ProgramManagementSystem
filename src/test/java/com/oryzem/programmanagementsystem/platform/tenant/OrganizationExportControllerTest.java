package com.oryzem.programmanagementsystem.platform.tenant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oryzem.programmanagementsystem.app.bootstrap.BootstrapDataService;
import com.oryzem.programmanagementsystem.platform.audit.AuditTrailService;
import io.micrometer.core.instrument.MeterRegistry;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = com.oryzem.programmanagementsystem.app.ProgramManagementSystemApplication.class)
@AutoConfigureMockMvc
class OrganizationExportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BootstrapDataService bootstrapDataService;

    @Autowired
    private AuditTrailService auditTrailService;

    @Autowired
    private MeterRegistry meterRegistry;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        bootstrapDataService.reset();
        auditTrailService.clear();
    }

    @Test
    void shouldStartAndCompleteExportForOffboardedOrganization() throws Exception {
        double offboardBefore = offboardCountFor("TEN-tenant-b");
        double exportRequestedBefore = exportRequestedCountFor("TEN-tenant-b");
        double exportCompletedBefore = exportCompletedCountFor("TEN-tenant-b");

        mockMvc.perform(delete("/api/access/organizations/tenant-b")
                        .with(internalAdmin()))
                .andExpect(status().isOk());

        Assertions.assertThat(offboardCountFor("TEN-tenant-b") - offboardBefore)
                .isEqualTo(1.0d);

        mockMvc.perform(post("/api/access/organizations/tenant-b/exports")
                        .with(internalAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "justification": "Manual export requested for retention handling."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organizationId").value("tenant-b"))
                .andExpect(jsonPath("$.dataExportStatus").value("EXPORT_IN_PROGRESS"))
                .andExpect(jsonPath("$.eligible").value(true));

        mockMvc.perform(patch("/api/access/organizations/tenant-b/exports")
                        .with(internalAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "justification": "Export completed after manual verification."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organizationId").value("tenant-b"))
                .andExpect(jsonPath("$.dataExportStatus").value("EXPORTED"))
                .andExpect(jsonPath("$.dataExportedAt").value(notNullValue()));

        mockMvc.perform(get("/api/access/organizations/tenant-b/exports")
                        .with(internalAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dataExportStatus").value("EXPORTED"))
                .andExpect(jsonPath("$.eligible").value(false));

        Assertions.assertThat(auditTrailService.findAll())
                .extracting(event -> event.eventType())
                .contains("ORGANIZATION_EXPORT_REQUESTED", "ORGANIZATION_EXPORT_COMPLETED");

        Assertions.assertThat(exportRequestedCountFor("TEN-tenant-b") - exportRequestedBefore)
                .isEqualTo(1.0d);

        Assertions.assertThat(exportCompletedCountFor("TEN-tenant-b") - exportCompletedBefore)
                .isEqualTo(1.0d);
    }

    @Test
    void shouldRejectExportForActiveOrganization() throws Exception {
        mockMvc.perform(post("/api/access/organizations/tenant-a/exports")
                        .with(internalAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "justification": "Premature export request."
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Only offboarded organizations can be exported."));
    }

    private org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor internalAdmin() {
        return jwt().jwt(jwt -> jwt
                        .claim("sub", "admin@oryzem.com-sub")
                        .claim("cognito:username", "admin@oryzem.com")
                        .claim("email", "admin@oryzem.com")
                        .claim("token_use", "access"))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private double offboardCountFor(String tenantId) {
        return countOperationalEvent("organization_offboard", tenantId);
    }

    private double exportRequestedCountFor(String tenantId) {
        return countOperationalEvent("organization_export_requested", tenantId);
    }

    private double exportCompletedCountFor(String tenantId) {
        return countOperationalEvent("organization_export_completed", tenantId);
    }

    private double countOperationalEvent(String eventType, String tenantId) {
        io.micrometer.core.instrument.Counter counter = meterRegistry.find("oryzem.operational.events")
                .tag("eventType", eventType)
                .tag("tenantId", tenantId)
                .counter();
        return counter != null ? counter.count() : 0.0d;
    }
}
