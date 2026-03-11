package com.oryzem.programmanagementsystem.operations;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OperationManagementControllerSecurityTest {

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
    void listOperationsShouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/operations"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void memberShouldCreateOperationInOwnTenant() throws Exception {
        mockMvc.perform(post("/api/operations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "New Trial",
                                  "description": "Operation created by member",
                                  "tenantId": "tenant-a",
                                  "tenantType": "EXTERNAL"
                                }
                                """)
                        .with(jwt().jwt(jwt -> jwt
                                        .claim("sub", "member-123")
                                        .claim("cognito:username", "member")
                                        .claim("tenant_id", "tenant-a")
                                        .claim("tenant_type", "EXTERNAL"))
                                .authorities(new SimpleGrantedAuthority("ROLE_MEMBER"))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/api/operations/")))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.tenantId").value("tenant-a"));
    }

    @Test
    void memberShouldEditOwnDraftOperation() throws Exception {
        mockMvc.perform(put("/api/operations/OP-TA-003")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Run @ Rate Updated",
                                  "description": "Updated by owner"
                                }
                                """)
                        .with(jwt().jwt(jwt -> jwt
                                        .claim("sub", "member-123")
                                        .claim("cognito:username", "member")
                                        .claim("tenant_id", "tenant-a")
                                        .claim("tenant_type", "EXTERNAL"))
                                .authorities(new SimpleGrantedAuthority("ROLE_MEMBER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Run @ Rate Updated"));
    }

    @Test
    void managerShouldApproveSubmittedOperationInOwnTenant() throws Exception {
        mockMvc.perform(post("/api/operations/OP-TA-002/approve")
                        .with(jwt().jwt(jwt -> jwt
                                        .claim("sub", "manager-123")
                                        .claim("cognito:username", "manager")
                                        .claim("tenant_id", "tenant-a")
                                        .claim("tenant_type", "EXTERNAL"))
                                .authorities(new SimpleGrantedAuthority("ROLE_MANAGER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.action").value("APPROVE"))
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void supportShouldReprocessCrossTenantOperationWithOverrideAndJustification() throws Exception {
        mockMvc.perform(post("/api/operations/OP-TB-001/reprocess")
                        .param("supportOverride", "true")
                        .param("justification", "Diagnosing supplier incident")
                        .with(jwt().jwt(jwt -> jwt
                                        .claim("sub", "support-123")
                                        .claim("cognito:username", "support")
                                        .claim("tenant_id", "internal-core")
                                        .claim("tenant_type", "INTERNAL"))
                                .authorities(new SimpleGrantedAuthority("ROLE_SUPPORT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.action").value("REPROCESS"))
                .andExpect(jsonPath("$.status").value("REPROCESSING"));
    }

    @Test
    void auditorShouldNotEditOperation() throws Exception {
        mockMvc.perform(put("/api/operations/OP-TB-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Blocked edit",
                                  "description": "Auditor cannot edit"
                                }
                                """)
                        .with(jwt().jwt(jwt -> jwt
                                        .claim("sub", "auditor-123")
                                        .claim("cognito:username", "auditor")
                                        .claim("tenant_id", "tenant-b")
                                        .claim("tenant_type", "EXTERNAL"))
                                .authorities(new SimpleGrantedAuthority("ROLE_AUDITOR"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }
}
