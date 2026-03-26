package com.oryzem.programmanagementsystem.platform.tenant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oryzem.programmanagementsystem.app.bootstrap.BootstrapDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = com.oryzem.programmanagementsystem.app.ProgramManagementSystemApplication.class)
@AutoConfigureMockMvc
class AccessOrganizationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BootstrapDataService bootstrapDataService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        bootstrapDataService.reset();
    }

    @Test
    void shouldListOrganizationsFromCoreAccessRoute() throws Exception {
        mockMvc.perform(get("/api/access/organizations")
                        .with(internalAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem("tenant-a")))
                .andExpect(jsonPath("$[*].name", hasItem("Tenant A")));
    }

    @Test
    void shouldCreateAndUpdateOrganizationFromCoreAccessRoute() throws Exception {
        String createResponse = mockMvc.perform(post("/api/access/organizations")
                        .with(internalAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Core Customer",
                                  "code": "CORE-CUSTOMER"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("CORE-CUSTOMER"))
                .andExpect(jsonPath("$.tenantType").value("EXTERNAL"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String organizationId = objectMapper.readTree(createResponse).get("id").asText();

        mockMvc.perform(put("/api/access/organizations/" + organizationId)
                        .with(internalAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                .content("""
                                {
                                  "name": "Core Customer Updated",
                                  "code": "CORE-CUSTOMER-UPD"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(organizationId))
                .andExpect(jsonPath("$.name").value("Core Customer Updated"))
                .andExpect(jsonPath("$.code").value("CORE-CUSTOMER-UPD"));
    }

    @Test
    void externalAdminShouldManageChildOrganizationThroughCoreAccessRoute() throws Exception {
        String childResponse = mockMvc.perform(post("/api/access/organizations")
                        .with(externalAdminTenantA())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Tenant A Tier 1",
                                  "code": "TENANT-A-T1",
                                  "parentOrganizationId": "tenant-a"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parentOrganizationId").value("tenant-a"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String organizationId = objectMapper.readTree(childResponse).get("id").asText();

        mockMvc.perform(put("/api/access/organizations/" + organizationId)
                        .with(externalAdminTenantA())
                        .contentType(MediaType.APPLICATION_JSON)
                .content("""
                                {
                                  "name": "Tenant A Tier 1 Updated",
                                  "code": "TENANT-A-T1-UPD"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(organizationId))
                .andExpect(jsonPath("$.customerOrganizationId").value("tenant-a"))
                .andExpect(jsonPath("$.code").value("TENANT-A-T1-UPD"));
    }

    private org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor internalAdmin() {
        return jwt().jwt(jwt -> jwt
                        .claim("sub", "admin@oryzem.com-sub")
                        .claim("cognito:username", "admin@oryzem.com")
                        .claim("email", "admin@oryzem.com")
                        .claim("token_use", "access"))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor externalAdminTenantA() {
        return jwt().jwt(jwt -> jwt
                        .claim("sub", "admin.a@tenant.com-sub")
                        .claim("cognito:username", "admin.a@tenant.com")
                        .claim("email", "admin.a@tenant.com")
                        .claim("token_use", "access"))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }
}
