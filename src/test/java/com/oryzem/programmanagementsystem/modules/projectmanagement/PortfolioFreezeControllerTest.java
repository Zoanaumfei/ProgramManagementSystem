package com.oryzem.programmanagementsystem.modules.projectmanagement;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = com.oryzem.programmanagementsystem.app.ProgramManagementSystemApplication.class)
@AutoConfigureMockMvc
class PortfolioFreezeControllerTest {

    private static final String FREEZE_MESSAGE =
            "Portfolio is temporarily unavailable while Oryzem focuses on the User + Organization core. "
                    + "Use /api/access/organizations for organization management.";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BootstrapDataService bootstrapDataService;

    @BeforeEach
    void setUp() {
        bootstrapDataService.reset();
    }

    @Test
    void portfolioEndpointsShouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/portfolio/programs"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void authenticatedPortfolioEndpointsShouldReturnServiceUnavailable() throws Exception {
        mockMvc.perform(get("/api/portfolio/programs")
                        .with(internalAdmin()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value(FREEZE_MESSAGE));

        mockMvc.perform(post("/api/portfolio/milestone-templates")
                        .with(internalAdmin())
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Template Freeze Check"
                                }
                                """))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value(FREEZE_MESSAGE));

        mockMvc.perform(get("/api/portfolio/organizations")
                        .with(internalAdmin()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value(FREEZE_MESSAGE));
    }

    @Test
    void portfolioFreezeShouldNotBreakCoreEndpoints() throws Exception {
        mockMvc.perform(get("/api/portfolio/programs")
                        .with(internalAdmin()))
                .andExpect(status().isServiceUnavailable());

        mockMvc.perform(get("/api/auth/me")
                        .with(internalAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeTenantId").value("TEN-internal-core"));

        mockMvc.perform(get("/api/access/users")
                        .with(internalAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        mockMvc.perform(get("/api/access/organizations")
                        .with(internalAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id").isNotEmpty());

        mockMvc.perform(get("/api/access/tenants/TEN-tenant-a/markets")
                        .with(internalAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    private org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor internalAdmin() {
        return jwt().jwt(jwt -> jwt
                        .claim("sub", "admin@oryzem.com-sub")
                        .claim("cognito:username", "admin@oryzem.com")
                        .claim("email", "admin@oryzem.com")
                        .claim("token_use", "access"))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }
}
