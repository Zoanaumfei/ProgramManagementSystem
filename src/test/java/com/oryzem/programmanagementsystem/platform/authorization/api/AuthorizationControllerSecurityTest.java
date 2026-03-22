package com.oryzem.programmanagementsystem.platform.authorization.api;

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

@SpringBootTest(classes = com.oryzem.programmanagementsystem.app.ProgramManagementSystemApplication.class)
@AutoConfigureMockMvc
class AuthorizationControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void authzCheckShouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/authz/check")
                        .param("module", "OPERATIONS")
                        .param("action", "VIEW"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void authzCheckShouldDenyManagerDeletingMemberInUsersModule() throws Exception {
        mockMvc.perform(get("/api/authz/check")
                        .param("module", "USERS")
                        .param("action", "DELETE")
                        .param("resourceTenantId", "tenant-a")
                        .param("targetRole", "MEMBER")
                        .with(jwt().jwt(jwt -> jwt
                                        .claim("sub", "manager-123")
                                        .claim("cognito:username", "manager")
                                        .claim("tenant_id", "tenant-a")
                                        .claim("tenant_type", "EXTERNAL"))
                                .authorities(new SimpleGrantedAuthority("ROLE_MANAGER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowed").value(false))
                .andExpect(jsonPath("$.crossTenant").value(false));
    }

    @Test
    void authzCheckShouldAllowInternalSupportViewingUsersCrossTenant() throws Exception {
        mockMvc.perform(get("/api/authz/check")
                        .param("module", "USERS")
                        .param("action", "VIEW")
                        .param("resourceTenantId", "tenant-b")
                        .with(jwt().jwt(jwt -> jwt
                                        .claim("sub", "support-123")
                                        .claim("cognito:username", "support")
                                        .claim("tenant_id", "internal-core")
                                        .claim("tenant_type", "INTERNAL"))
                                .authorities(new SimpleGrantedAuthority("ROLE_SUPPORT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowed").value(true))
                .andExpect(jsonPath("$.crossTenant").value(true));
    }

    @Test
    void authzCheckShouldDenySupportCrossTenantImpersonationWithoutControls() throws Exception {
        mockMvc.perform(get("/api/authz/check")
                        .param("module", "SUPPORT")
                        .param("action", "IMPERSONATE")
                        .param("resourceTenantId", "tenant-b")
                        .with(jwt().jwt(jwt -> jwt
                                        .claim("sub", "support-123")
                                        .claim("cognito:username", "support")
                                        .claim("tenant_id", "tenant-a")
                                        .claim("tenant_type", "INTERNAL"))
                                .authorities(new SimpleGrantedAuthority("ROLE_SUPPORT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowed").value(false))
                .andExpect(jsonPath("$.crossTenant").value(true));
    }
}

