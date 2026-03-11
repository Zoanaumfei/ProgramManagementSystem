package com.oryzem.programmanagementsystem.users;

import com.oryzem.programmanagementsystem.audit.AuditTrailService;
import com.oryzem.programmanagementsystem.bootstrap.BootstrapDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserManagementControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuditTrailService auditTrailService;

    @Autowired
    private BootstrapDataService bootstrapDataService;

    @BeforeEach
    void setUp() {
        bootstrapDataService.reset();
        auditTrailService.clear();
    }

    @Test
    void listUsersShouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void managerShouldSeeOnlyOwnTenantUsers() throws Exception {
        mockMvc.perform(get("/api/users")
                        .with(jwt().jwt(jwt -> jwt
                                        .claim("sub", "manager-123")
                                        .claim("cognito:username", "manager")
                                        .claim("tenant_id", "tenant-a")
                                        .claim("tenant_type", "EXTERNAL"))
                                .authorities(new SimpleGrantedAuthority("ROLE_MANAGER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].tenantId").value("tenant-a"))
                .andExpect(jsonPath("$[1].tenantId").value("tenant-a"));
    }

    @Test
    void memberShouldNotListUsers() throws Exception {
        mockMvc.perform(get("/api/users")
                        .with(jwt().jwt(jwt -> jwt
                                        .claim("sub", "member-123")
                                        .claim("cognito:username", "member")
                                        .claim("tenant_id", "tenant-a")
                                        .claim("tenant_type", "EXTERNAL"))
                                .authorities(new SimpleGrantedAuthority("ROLE_MEMBER"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    void managerShouldCreateMemberInOwnTenant() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "New Member",
                                  "email": "new.member@tenant.com",
                                  "role": "MEMBER",
                                  "tenantId": "tenant-a",
                                  "tenantType": "EXTERNAL"
                                }
                                """)
                        .with(jwt().jwt(jwt -> jwt
                                        .claim("sub", "manager-123")
                                        .claim("cognito:username", "manager")
                                        .claim("tenant_id", "tenant-a")
                                        .claim("tenant_type", "EXTERNAL"))
                                .authorities(new SimpleGrantedAuthority("ROLE_MANAGER"))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/api/users/")))
                .andExpect(jsonPath("$.role").value("MEMBER"))
                .andExpect(jsonPath("$.tenantId").value("tenant-a"))
                .andExpect(jsonPath("$.status").value("INVITED"));
    }

    @Test
    void managerShouldNotCreateAdmin() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "Escalated Admin",
                                  "email": "admin2@tenant.com",
                                  "role": "ADMIN",
                                  "tenantId": "tenant-a",
                                  "tenantType": "EXTERNAL"
                                }
                                """)
                        .with(jwt().jwt(jwt -> jwt
                                        .claim("sub", "manager-123")
                                        .claim("cognito:username", "manager")
                                        .claim("tenant_id", "tenant-a")
                                        .claim("tenant_type", "EXTERNAL"))
                                .authorities(new SimpleGrantedAuthority("ROLE_MANAGER"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Target role")));
    }

    @Test
    void supportShouldResetAccessCrossTenantWithOverrideAndJustification() throws Exception {
        mockMvc.perform(post("/api/users/USR-EXT-B-MEM-001/reset-access")
                        .param("supportOverride", "true")
                        .param("justification", "Customer requested incident recovery")
                        .with(jwt().jwt(jwt -> jwt
                                        .claim("sub", "support-123")
                                        .claim("cognito:username", "support")
                                        .claim("tenant_id", "internal-core")
                                        .claim("tenant_type", "INTERNAL"))
                                .authorities(new SimpleGrantedAuthority("ROLE_SUPPORT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.action").value("RESET_ACCESS"))
                .andExpect(jsonPath("$.status").value("OK"));
    }

    @Test
    void adminShouldDeleteAdminUser() throws Exception {
        mockMvc.perform(delete("/api/users/USR-ADMIN-002")
                        .with(jwt().jwt(jwt -> jwt
                                        .claim("sub", "admin-123")
                                        .claim("cognito:username", "admin")
                                        .claim("tenant_id", "internal-core")
                                        .claim("tenant_type", "INTERNAL"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNoContent());
    }
}
