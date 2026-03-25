package com.oryzem.programmanagementsystem.platform.users.api;

import com.oryzem.programmanagementsystem.app.bootstrap.BootstrapDataService;
import com.oryzem.programmanagementsystem.platform.audit.AuditTrailService;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = com.oryzem.programmanagementsystem.app.ProgramManagementSystemApplication.class)
@AutoConfigureMockMvc
class LegacyUsersOnlyWarningFeatureFlagTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BootstrapDataService bootstrapDataService;

    @Autowired
    private AuditTrailService auditTrailService;

    @Autowired
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        bootstrapDataService.reset();
        auditTrailService.clear();
    }

    @Test
    void legacyUsersShouldExposeDeprecationHeadersAndAdoptionReport() throws Exception {
        mockMvc.perform(get("/api/users")
                        .with(externalAdminTenantA()))
                .andExpect(status().isOk())
                .andExpect(header().string("Deprecation", "true"))
                .andExpect(header().string("X-Legacy-Users-Stage", "ONLY_WARNING"))
                .andExpect(header().string("X-Legacy-Users-Write-Enabled", "true"));

        mockMvc.perform(get("/api/access/users/USR-EXT-A-MEM-001/memberships")
                        .with(internalAdmin()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/access/legacy-users/deprecation-status")
                        .with(internalAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usersLegacyUiEnabled").value(true))
                .andExpect(jsonPath("$.usersLegacyReadEnabled").value(true))
                .andExpect(jsonPath("$.usersLegacyWriteEnabled").value(true))
                .andExpect(jsonPath("$.currentStage").value("ONLY_WARNING"))
                .andExpect(jsonPath("$.replacementPath").value("/api/access/users/{userId}/memberships"));

        mockMvc.perform(get("/api/access/legacy-users/adoption-report")
                        .param("trailingDays", "7")
                        .with(internalAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.legacyOperations").value(1))
                .andExpect(jsonPath("$.membershipOperations").value(1))
                .andExpect(jsonPath("$.legacySharePercent").value(50.0))
                .andExpect(jsonPath("$.operationBreakdown[?(@.apiFamily == 'legacy_users')].operation").value(hasItem("list")))
                .andExpect(jsonPath("$.operationBreakdown[?(@.apiFamily == 'membership_users')].operation").value(hasItem("list")))
                .andExpect(jsonPath("$.roleBreakdown[?(@.apiFamily == 'legacy_users')].actorRole").value(hasItem("ADMIN")))
                .andExpect(jsonPath("$.tenantsStillDependentOnLegacy[0].legacyOperations").value(greaterThanOrEqualTo(1)));

        Double legacyCounter = meterRegistry.get("pms.access.adoption.api.usage")
                .tag("api_family", "legacy_users")
                .tag("operation", "list")
                .tag("tenant", "ten-tenant-a")
                .tag("actor_role", "admin")
                .counter()
                .count();
        Double membershipCounter = meterRegistry.get("pms.access.adoption.api.usage")
                .tag("api_family", "membership_users")
                .tag("operation", "list")
                .tag("tenant", "ten-tenant-a")
                .tag("actor_role", "admin")
                .counter()
                .count();
        assertThat(legacyCounter).isGreaterThanOrEqualTo(1.0d);
        assertThat(membershipCounter).isGreaterThanOrEqualTo(1.0d);
    }

    @Test
    void supportOperatorShouldReadDeprecationTelemetry() throws Exception {
        mockMvc.perform(get("/api/access/legacy-users/deprecation-status")
                        .with(internalSupport()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStage").value("ONLY_WARNING"));

        mockMvc.perform(get("/api/access/legacy-users/adoption-report")
                        .param("trailingDays", "7")
                        .with(internalSupport()))
                .andExpect(status().isOk());
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor internalAdmin() {
        return jwt().jwt(jwt -> jwt
                        .claim("sub", "admin-123")
                        .claim("cognito:username", "admin")
                        .claim("email", "admin@oryzem.com")
                        .claim("tenant_id", "internal-core")
                        .claim("tenant_type", "INTERNAL"))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor externalAdminTenantA() {
        return jwt().jwt(jwt -> jwt
                        .claim("sub", "admin-a-123")
                        .claim("cognito:username", "admin.a")
                        .claim("email", "admin.a@tenant.com")
                        .claim("tenant_id", "tenant-a")
                        .claim("tenant_type", "EXTERNAL"))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor internalSupport() {
        return jwt().jwt(jwt -> jwt
                        .claim("sub", "support-123")
                        .claim("cognito:username", "support")
                        .claim("email", "support@oryzem.com")
                        .claim("tenant_id", "internal-core")
                        .claim("tenant_type", "INTERNAL"))
                .authorities(new SimpleGrantedAuthority("ROLE_SUPPORT"));
    }
}
