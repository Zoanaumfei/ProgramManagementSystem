package com.oryzem.programmanagementsystem.platform.access;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oryzem.programmanagementsystem.app.bootstrap.BootstrapDataService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = com.oryzem.programmanagementsystem.app.ProgramManagementSystemApplication.class)
@AutoConfigureMockMvc
class AccessAdministrationControllerTest {

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
    void internalAdminShouldCreateAndListMultipleMemberships() throws Exception {
        String response = mockMvc.perform(post("/api/access/users/USR-EXT-A-MEM-001/memberships")
                        .with(internalAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantId": "TEN-tenant-b",
                                  "organizationId": "tenant-b",
                                  "roles": ["SUPPORT"],
                                  "defaultMembership": false,
                                  "status": "ACTIVE"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tenantId").value("TEN-tenant-b"))
                .andExpect(jsonPath("$.organizationId").value("tenant-b"))
                .andExpect(jsonPath("$.roles[0]").value("SUPPORT"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String membershipId = objectMapper.readTree(response).get("id").asText();

        mockMvc.perform(get("/api/access/users/USR-EXT-A-MEM-001/memberships")
                        .with(internalAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(membershipId)))
                .andExpect(jsonPath("$[?(@.id == '" + membershipId + "')].tenantId").value(hasItem("TEN-tenant-b")));
    }

    @Test
    void externalAdminShouldNotCreateMembershipOutsideOwnTenant() throws Exception {
        mockMvc.perform(post("/api/access/users/USR-EXT-A-MEM-001/memberships")
                        .with(externalAdminTenantA())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantId": "TEN-tenant-b",
                                  "organizationId": "tenant-b",
                                  "roles": ["SUPPORT"],
                                  "defaultMembership": false,
                                  "status": "ACTIVE"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldActivateMembershipAndResolveContextFromHeaderOrDefault() throws Exception {
        String createResponse = mockMvc.perform(post("/api/access/users/USR-EXT-A-MEM-001/memberships")
                        .with(internalAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantId": "TEN-tenant-b",
                                  "organizationId": "tenant-b",
                                  "roles": ["SUPPORT"],
                                  "defaultMembership": false,
                                  "status": "ACTIVE"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String membershipId = objectMapper.readTree(createResponse).get("id").asText();

        mockMvc.perform(get("/api/auth/me")
                        .with(tenantAUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeTenantId").value("TEN-tenant-a"))
                .andExpect(jsonPath("$.activeOrganizationId").value("tenant-a"));

        mockMvc.perform(get("/api/auth/me")
                        .header("X-Access-Context", membershipId)
                        .with(tenantAUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.membershipId").value(membershipId))
                .andExpect(jsonPath("$.activeTenantId").value("TEN-tenant-b"))
                .andExpect(jsonPath("$.activeOrganizationId").value("tenant-b"))
                .andExpect(jsonPath("$.roles[0]").value("SUPPORT"));

        mockMvc.perform(post("/api/access/context/activate")
                        .with(tenantAUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "membershipId": "%s",
                                  "makeDefault": true
                                }
                                """.formatted(membershipId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.membershipId").value(membershipId))
                .andExpect(jsonPath("$.activeTenantId").value("TEN-tenant-b"));

        mockMvc.perform(get("/api/auth/me")
                        .with(tenantAUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.membershipId").value(membershipId))
                .andExpect(jsonPath("$.activeTenantId").value("TEN-tenant-b"));
    }

    @Test
    void shouldManageTenantMarketsAndBlockInactivationWhenMarketIsInUse() throws Exception {
        String marketResponse = mockMvc.perform(post("/api/access/tenants/TEN-tenant-a/markets")
                        .with(internalAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "BR",
                                  "name": "Brazil",
                                  "currencyCode": "BRL",
                                  "languageCode": "pt-BR",
                                  "timezone": "America/Sao_Paulo",
                                  "status": "ACTIVE"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("BR"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String marketId = objectMapper.readTree(marketResponse).get("id").asText();

        mockMvc.perform(get("/api/access/tenants/TEN-tenant-a/markets")
                        .with(internalAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].code", hasItem("BR")));

        mockMvc.perform(put("/api/access/tenants/TEN-tenant-a/markets/" + marketId)
                        .with(internalAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "BR-OPS",
                                  "name": "Brazil Operations",
                                  "currencyCode": "BRL",
                                  "languageCode": "pt-BR",
                                  "timezone": "America/Sao_Paulo",
                                  "status": "ACTIVE"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("BR-OPS"));

        mockMvc.perform(post("/api/access/users/USR-EXT-A-MEM-001/memberships")
                        .with(internalAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantId": "TEN-tenant-a",
                                  "organizationId": "tenant-a",
                                  "marketId": "%s",
                                  "roles": ["MANAGER"],
                                  "defaultMembership": false,
                                  "status": "ACTIVE"
                                }
                                """.formatted(marketId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.marketId").value(marketId));

        mockMvc.perform(delete("/api/access/tenants/TEN-tenant-a/markets/" + marketId)
                        .with(internalAdmin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Market cannot be inactivated while referenced by an active membership."));
    }

    @Test
    void shouldInactivateUnusedTenantMarket() throws Exception {
        String marketResponse = mockMvc.perform(post("/api/access/tenants/TEN-tenant-a/markets")
                        .with(internalAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "AR",
                                  "name": "Argentina",
                                  "currencyCode": "ARS",
                                  "languageCode": "es-AR",
                                  "timezone": "America/Argentina/Buenos_Aires",
                                  "status": "ACTIVE"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String marketId = objectMapper.readTree(marketResponse).get("id").asText();

        mockMvc.perform(delete("/api/access/tenants/TEN-tenant-a/markets/" + marketId)
                        .with(internalAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    private RequestPostProcessor internalAdmin() {
        return jwt().jwt(jwt -> jwt
                        .claim("sub", "admin-123")
                        .claim("cognito:username", "admin")
                        .claim("email", "admin@oryzem.com")
                        .claim("tenant_id", "internal-core")
                        .claim("tenant_type", "INTERNAL"))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private RequestPostProcessor externalAdminTenantA() {
        return jwt().jwt(jwt -> jwt
                        .claim("sub", "admin-a-123")
                        .claim("cognito:username", "admin.a")
                        .claim("email", "admin.a@tenant.com")
                        .claim("tenant_id", "tenant-a")
                        .claim("tenant_type", "EXTERNAL"))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private RequestPostProcessor tenantAUser() {
        return jwt().jwt(jwt -> jwt
                        .claim("sub", "member-a-sub")
                        .claim("cognito:username", "member.a@tenant.com")
                        .claim("email", "member.a@tenant.com")
                        .claim("tenant_id", "tenant-a")
                        .claim("tenant_type", "EXTERNAL"))
                .authorities(new SimpleGrantedAuthority("ROLE_MEMBER"));
    }
}
