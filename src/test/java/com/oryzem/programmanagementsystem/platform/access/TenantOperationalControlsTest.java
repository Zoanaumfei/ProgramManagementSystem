package com.oryzem.programmanagementsystem.platform.access;

import io.micrometer.core.instrument.MeterRegistry;
import com.oryzem.programmanagementsystem.app.bootstrap.BootstrapDataService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = com.oryzem.programmanagementsystem.app.ProgramManagementSystemApplication.class,
        properties = {
                "app.multitenancy.rate-limit.window-seconds=60",
                "app.multitenancy.rate-limit.internal-max-requests=2",
                "app.multitenancy.rate-limit.standard-max-requests=2",
                "app.multitenancy.rate-limit.enterprise-max-requests=2",
                "app.multitenancy.quota.standard.max-active-memberships=4"
        })
@AutoConfigureMockMvc
class TenantOperationalControlsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BootstrapDataService bootstrapDataService;

    @Autowired
    private TenantRateLimitingFilter tenantRateLimitingFilter;

    @Autowired
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        bootstrapDataService.reset();
        tenantRateLimitingFilter.clearCounters();
    }

    @Test
    void shouldRateLimitPerTenant() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .with(jwtFor("member.a@tenant.com", "ROLE_MEMBER")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/auth/me")
                        .with(jwtFor("member.a@tenant.com", "ROLE_MEMBER")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/auth/me")
                        .with(jwtFor("member.a@tenant.com", "ROLE_MEMBER")))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value("Tenant rate limit exceeded."))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.path").value("/api/auth/me"))
                .andExpect(jsonPath("$.correlationId").isNotEmpty());

        org.assertj.core.api.Assertions.assertThat(meterRegistry.get("oryzem.operational.http.responses")
                        .tag("tenantId", "TEN-tenant-a")
                        .tag("tenantTier", "STANDARD")
                        .tag("path", "/api/auth/me")
                        .tag("status", "429")
                        .tag("category", "rate_limit")
                        .counter()
                        .count())
                .isEqualTo(1.0d);
    }

    @Test
    void shouldApplyMembershipQuotaByTenantTier() throws Exception {
        mockMvc.perform(post("/api/access/users")
                        .with(jwtFor("admin.a@tenant.com", "ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "Quota User",
                                  "email": "quota.user@tenant.com",
                                  "organizationId": "tenant-a",
                                  "roles": ["MEMBER"]
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Active membership quota reached for tenant tier."));

        org.assertj.core.api.Assertions.assertThat(meterRegistry.get("oryzem.operational.http.responses")
                        .tag("tenantId", "TEN-tenant-a")
                        .tag("tenantTier", "STANDARD")
                        .tag("path", "quota")
                        .tag("status", "409")
                        .tag("category", "quota_active_memberships")
                        .counter()
                        .count())
                .isEqualTo(1.0d);
    }

    private org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtFor(
            String username,
            String authority) {
        return jwt().jwt(jwt -> jwt
                        .claim("sub", username + "-sub")
                        .claim("cognito:username", username)
                        .claim("email", username)
                        .claim("token_use", "access"))
                .authorities(new SimpleGrantedAuthority(authority));
    }
}
