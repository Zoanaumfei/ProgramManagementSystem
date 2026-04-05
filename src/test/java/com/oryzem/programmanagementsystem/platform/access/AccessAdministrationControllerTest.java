package com.oryzem.programmanagementsystem.platform.access;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oryzem.programmanagementsystem.app.bootstrap.BootstrapDataService;
import com.oryzem.programmanagementsystem.platform.audit.AuditTrailService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

    @Autowired
    private TenantGovernanceService tenantGovernanceService;

    @Autowired
    private AuditTrailService auditTrailService;

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
    void shouldCreateUserAlreadyProvisionedWithInitialMembership() throws Exception {
        String createUserResponse = mockMvc.perform(post("/api/access/users")
                        .with(externalAdminTenantA())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "Bootstrap Pending",
                                  "email": "bootstrap.pending@tenant.com",
                                  "organizationId": "tenant-a",
                                  "roles": ["MEMBER"]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.membershipAssigned").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String userId = objectMapper.readTree(createUserResponse).get("id").asText();

        mockMvc.perform(post("/api/access/users/" + userId + "/bootstrap-membership")
                        .with(externalAdminTenantA())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "organizationId": "tenant-a",
                                  "roles": ["SUPPORT"],
                                  "status": "ACTIVE"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Bootstrap membership is allowed only for users without memberships."));
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
                .andExpect(jsonPath("$.userId").value("USR-EXT-A-MEM-001"))
                .andExpect(jsonPath("$.activeTenantId").value("TEN-tenant-a"))
                .andExpect(jsonPath("$.activeTenantName").value("Tenant A"))
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
    void shouldUpdateAndInactivateMembership() throws Exception {
        String createResponse = mockMvc.perform(post("/api/access/users/USR-EXT-A-MEM-001/memberships")
                        .with(internalAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantId": "TEN-tenant-a",
                                  "organizationId": "tenant-a",
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

        mockMvc.perform(put("/api/access/users/USR-EXT-A-MEM-001/memberships/" + membershipId)
                        .with(internalAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantId": "TEN-tenant-a",
                                  "organizationId": "tenant-a",
                                  "roles": ["MANAGER"],
                                  "defaultMembership": false,
                                  "status": "ACTIVE"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(membershipId))
                .andExpect(jsonPath("$.roles[0]").value("MANAGER"));

        mockMvc.perform(delete("/api/access/users/USR-EXT-A-MEM-001/memberships/" + membershipId)
                        .with(internalAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(membershipId))
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test
    void shouldUpdateMembershipKeepingSameRoleWithoutDuplicateConstraint() throws Exception {
        String createResponse = mockMvc.perform(post("/api/access/users/USR-EXT-A-MEM-001/memberships")
                        .with(internalAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantId": "TEN-tenant-a",
                                  "organizationId": "tenant-a",
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

        mockMvc.perform(put("/api/access/users/USR-EXT-A-MEM-001/memberships/" + membershipId)
                        .with(internalAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantId": "TEN-tenant-a",
                                  "organizationId": "tenant-a",
                                  "roles": ["SUPPORT"],
                                  "defaultMembership": false,
                                  "status": "ACTIVE"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(membershipId))
                .andExpect(jsonPath("$.roles[0]").value("SUPPORT"));
    }

    @Test
    void shouldListVisibleTenantsForInternalAdmin() throws Exception {
        mockMvc.perform(get("/api/access/tenants")
                        .with(internalAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem("TEN-tenant-a")))
                .andExpect(jsonPath("$[*].name", hasItem("Tenant A")));
    }

    @Test
    void externalAdminWithSupportRoleShouldNotListInternalOrSiblingTenants() throws Exception {
        mockMvc.perform(get("/api/access/tenants")
                        .with(externalAdminSupportTenantA()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("TEN-tenant-a"));
    }

    @Test
    void visibleTenantsShouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/access/tenants"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.correlationId").isNotEmpty());
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

    @Test
    void externalAdminWithSupportRoleShouldNotCreateMarketOutsideOwnTenant() throws Exception {
        mockMvc.perform(post("/api/access/tenants/TEN-tenant-b/markets")
                        .with(externalAdminSupportTenantA())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "INT-BR",
                                  "name": "Internal Brazil",
                                  "currencyCode": "BRL",
                                  "languageCode": "pt-BR",
                                  "timezone": "America/Sao_Paulo",
                                  "status": "ACTIVE"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldChangeTenantServiceTierAndRecordAudit() throws Exception {
        mockMvc.perform(patch("/api/access/tenants/TEN-tenant-a/service-tier")
                        .with(internalAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "serviceTier": "ENTERPRISE",
                                  "justification": "Increase tenant capacity for seasonal load."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId").value("TEN-tenant-a"))
                .andExpect(jsonPath("$.previousServiceTier").value("STANDARD"))
                .andExpect(jsonPath("$.serviceTier").value("ENTERPRISE"));

        Assertions.assertThat(tenantGovernanceService.resolveTier("TEN-tenant-a"))
                .isEqualTo(TenantServiceTier.ENTERPRISE);
        Assertions.assertThat(auditTrailService.findAll())
                .extracting(event -> event.eventType())
                .contains("TENANT_SERVICE_TIER_CHANGE");
    }

    @Test
    void supportShouldChangeTenantServiceTierWithJustification() throws Exception {
        mockMvc.perform(patch("/api/access/tenants/TEN-tenant-b/service-tier")
                        .with(internalSupport())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "serviceTier": "ENTERPRISE",
                                  "justification": "Support-approved upgrade after incident review."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId").value("TEN-tenant-b"))
                .andExpect(jsonPath("$.previousServiceTier").value("STANDARD"))
                .andExpect(jsonPath("$.serviceTier").value("ENTERPRISE"));
    }

    private RequestPostProcessor internalAdmin() {
        return jwt().jwt(jwt -> jwt
                        .claim("sub", "admin@oryzem.com-sub")
                        .claim("cognito:username", "admin@oryzem.com")
                        .claim("email", "admin@oryzem.com"))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private RequestPostProcessor externalAdminTenantA() {
        return jwt().jwt(jwt -> jwt
                        .claim("sub", "admin.a@tenant.com-sub")
                        .claim("cognito:username", "admin.a@tenant.com")
                        .claim("email", "admin.a@tenant.com"))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private RequestPostProcessor internalSupport() {
        return jwt().jwt(jwt -> jwt
                        .claim("sub", "support@oryzem.com-sub")
                        .claim("cognito:username", "support@oryzem.com")
                        .claim("email", "support@oryzem.com")
                        .claim("token_use", "access"))
                .authorities(new SimpleGrantedAuthority("ROLE_SUPPORT"));
    }

    private RequestPostProcessor externalAdminSupportTenantA() {
        return jwt().jwt(jwt -> jwt
                        .claim("sub", "admin.a@tenant.com-sub")
                        .claim("cognito:username", "admin.a@tenant.com")
                        .claim("email", "admin.a@tenant.com")
                        .claim("token_use", "access"))
                .authorities(
                        new SimpleGrantedAuthority("ROLE_ADMIN"),
                        new SimpleGrantedAuthority("ROLE_SUPPORT"));
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
