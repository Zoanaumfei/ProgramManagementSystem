package com.oryzem.programmanagementsystem.platform.users.api;

import com.oryzem.programmanagementsystem.app.bootstrap.BootstrapDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = com.oryzem.programmanagementsystem.app.ProgramManagementSystemApplication.class,
        properties = {
                "app.features.users-legacy.read-enabled=false",
                "app.features.users-legacy.write-enabled=false",
                "app.features.users-legacy.ui-enabled=false"
        })
@AutoConfigureMockMvc
class LegacyUsersOffByDefaultFeatureFlagTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BootstrapDataService bootstrapDataService;

    @BeforeEach
    void setUp() {
        bootstrapDataService.reset();
    }

    @Test
    void legacyUsersShouldBeHiddenWhenReadAndWriteAreDisabled() throws Exception {
        mockMvc.perform(get("/api/users")
                        .with(externalAdminTenantA()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.currentStage").value("OFF_BY_DEFAULT"))
                .andExpect(jsonPath("$.operation").value("list"));
    }

    @Test
    void membershipFirstFlowShouldRemainAvailableWhenLegacyIsOffByDefault() throws Exception {
        mockMvc.perform(get("/api/access/users/USR-EXT-A-MEM-001/memberships")
                        .with(internalAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value("USR-EXT-A-MEM-001"));

        mockMvc.perform(get("/api/access/legacy-users/deprecation-status")
                        .with(internalAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usersLegacyUiEnabled").value(false))
                .andExpect(jsonPath("$.usersLegacyReadEnabled").value(false))
                .andExpect(jsonPath("$.usersLegacyWriteEnabled").value(false))
                .andExpect(jsonPath("$.currentStage").value("OFF_BY_DEFAULT"));
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
}
