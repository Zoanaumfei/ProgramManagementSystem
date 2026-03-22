package com.oryzem.programmanagementsystem.modules.reports;

import com.oryzem.programmanagementsystem.platform.audit.AuditTrailService;
import com.oryzem.programmanagementsystem.platform.audit.RequestCorrelationFilter;
import com.oryzem.programmanagementsystem.app.bootstrap.BootstrapDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = com.oryzem.programmanagementsystem.app.ProgramManagementSystemApplication.class)
@AutoConfigureMockMvc
class ReportControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BootstrapDataService bootstrapDataService;

    @Autowired
    private AuditTrailService auditTrailService;

    @BeforeEach
    void setUp() {
        bootstrapDataService.reset();
        auditTrailService.clear();
    }

    @Test
    void summaryShouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/reports/summary"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void memberShouldViewOwnTenantSummary() throws Exception {
        mockMvc.perform(get("/api/reports/summary")
                        .with(jwt().jwt(jwt -> jwt
                                        .claim("sub", "member-123")
                                        .claim("cognito:username", "member")
                                        .claim("tenant_id", "tenant-a")
                                        .claim("tenant_type", "EXTERNAL"))
                                .authorities(new SimpleGrantedAuthority("ROLE_MEMBER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId").value("tenant-a"))
                .andExpect(jsonPath("$.totalUsers").value(3))
                .andExpect(jsonPath("$.totalOperations").value(3))
                .andExpect(jsonPath("$.usersByRole.ADMIN").value(1))
                .andExpect(jsonPath("$.usersByRole.MANAGER").value(1))
                .andExpect(jsonPath("$.operationsByStatus.DRAFT").value(2));
    }

    @Test
    void supportShouldViewCrossTenantSummaryWithOverrideAndJustification() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/reports/summary")
                        .param("tenantId", "tenant-b")
                        .param("supportOverride", "true")
                        .param("justification", "Supplier escalation")
                        .with(jwt().jwt(jwt -> jwt
                                        .claim("sub", "support-123")
                                        .claim("cognito:username", "support")
                                        .claim("tenant_id", "internal-core")
                                        .claim("tenant_type", "INTERNAL"))
                                .authorities(new SimpleGrantedAuthority("ROLE_SUPPORT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId").value("tenant-b"))
                .andExpect(jsonPath("$.totalUsers").value(4))
                .andExpect(jsonPath("$.totalOperations").value(2))
                .andReturn();

        String correlationId = result.getResponse().getHeader(RequestCorrelationFilter.HEADER_NAME);

        org.assertj.core.api.Assertions.assertThat(auditTrailService.findAll())
                .hasSize(1)
                .first()
                .extracting(
                        event -> event.eventType(),
                        event -> event.targetTenantId(),
                        event -> event.crossTenant(),
                        event -> event.sourceModule(),
                        event -> event.correlationId())
                .containsExactly("REPORT_SUMMARY_VIEW", "tenant-b", true, "REPORTS", correlationId);
    }

    @Test
    void supportShouldNotExportSensitiveReportWithoutMaskedView() throws Exception {
        mockMvc.perform(get("/api/reports/operations/export")
                        .param("tenantId", "tenant-b")
                        .param("includeSensitiveData", "true")
                        .param("supportOverride", "true")
                        .param("justification", "Supplier escalation")
                        .with(jwt().jwt(jwt -> jwt
                                        .claim("sub", "support-123")
                                        .claim("cognito:username", "support")
                                        .claim("tenant_id", "internal-core")
                                        .claim("tenant_type", "INTERNAL"))
                                .authorities(new SimpleGrantedAuthority("ROLE_SUPPORT"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Sensitive data requires masked access."));
    }

    @Test
    void auditorShouldExportMaskedOperationsReport() throws Exception {
        mockMvc.perform(get("/api/reports/operations/export")
                        .param("includeSensitiveData", "true")
                        .param("maskedView", "true")
                        .with(jwt().jwt(jwt -> jwt
                                        .claim("sub", "auditor-123")
                                        .claim("cognito:username", "auditor")
                                        .claim("tenant_id", "tenant-b")
                                        .claim("tenant_type", "EXTERNAL"))
                                .authorities(new SimpleGrantedAuthority("ROLE_AUDITOR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId").value("tenant-b"))
                .andExpect(jsonPath("$.masked").value(true))
                .andExpect(jsonPath("$.items[0].description").value("[MASKED]"))
                .andExpect(jsonPath("$.items[0].createdBy").value(org.hamcrest.Matchers.containsString("***")));
    }
}


