package com.oryzem.programmanagementsystem.platform.users.api;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = com.oryzem.programmanagementsystem.app.ProgramManagementSystemApplication.class,
        properties = {
                "app.features.users-legacy.read-enabled=true",
                "app.features.users-legacy.write-enabled=false",
                "app.features.users-legacy.ui-enabled=true"
        })
@AutoConfigureMockMvc
class LegacyUsersReadOnlyFeatureFlagTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BootstrapDataService bootstrapDataService;

    @BeforeEach
    void setUp() {
        bootstrapDataService.reset();
    }

    @Test
    void legacyReadsShouldRemainAvailableInReadOnlyMode() throws Exception {
        mockMvc.perform(get("/api/users")
                        .with(externalAdminTenantA()))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Legacy-Users-Stage", "READ_ONLY"))
                .andExpect(header().string("X-Legacy-Users-Read-Enabled", "true"))
                .andExpect(header().string("X-Legacy-Users-Write-Enabled", "false"));
    }

    @Test
    void legacyWritesShouldBeBlockedForAdminAndSupportInReadOnlyMode() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "Blocked Member",
                                  "email": "blocked.member@tenant.com",
                                  "role": "MEMBER",
                                  "organizationId": "tenant-a"
                                }
                                """)
                        .with(externalAdminTenantA()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.currentStage").value("READ_ONLY"))
                .andExpect(jsonPath("$.operation").value("create"))
                .andExpect(jsonPath("$.replacementPath").value("/api/access/users/{userId}/memberships"));

        mockMvc.perform(post("/api/users/USR-EXT-B-MEM-001/reset-access")
                        .with(internalSupport()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.currentStage").value("READ_ONLY"))
                .andExpect(jsonPath("$.operation").value("reset_access"));
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
