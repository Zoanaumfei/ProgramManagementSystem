package com.oryzem.programmanagementsystem.platform.tenant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oryzem.programmanagementsystem.app.bootstrap.BootstrapDataService;
import com.oryzem.programmanagementsystem.platform.audit.AuditTrailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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

    @Autowired
    private AuditTrailService auditTrailService;

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
                                  "cnpj": "11.222.333/0001-81"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cnpj").value("11222333000181"))
                .andExpect(jsonPath("$.tenantType").value("EXTERNAL"))
                .andExpect(jsonPath("$.reused").value(false))
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
                                  "cnpj": "22.333.444/0001-81"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(organizationId))
                .andExpect(jsonPath("$.name").value("Core Customer Updated"))
                .andExpect(jsonPath("$.cnpj").value("22333444000181"));
    }

    @Test
    void externalAdminShouldCreateSupplierRelationshipThroughCoreAccessRoute() throws Exception {
        String supplierResponse = mockMvc.perform(post("/api/access/organizations")
                        .with(externalAdminTenantA())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Gestamp",
                                  "cnpj": "33.444.555/0001-81",
                                  "localOrganizationCode": "GESTAMP"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cnpj").value("33444555000181"))
                .andExpect(jsonPath("$.reused").value(false))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String organizationId = objectMapper.readTree(supplierResponse).get("id").asText();

        mockMvc.perform(put("/api/access/organizations/" + organizationId)
                        .with(externalAdminTenantA())
                        .contentType(MediaType.APPLICATION_JSON)
                .content("""
                                {
                                  "name": "Gestamp Updated",
                                  "cnpj": "33.444.555/0001-81"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(organizationId));

        mockMvc.perform(get("/api/access/organizations/tenant-a/relationships")
                        .with(externalAdminTenantA()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].targetOrganizationId", hasItem(organizationId)))
                .andExpect(jsonPath("$[*].relationshipType", hasItem("CUSTOMER_SUPPLIER")))
                .andExpect(jsonPath("$[?(@.targetOrganizationId == '" + organizationId + "')].localOrganizationCode")
                        .value(hasItem("GESTAMP")));
    }

    @Test
    void externalAdminShouldReuseExistingOrganizationByCnpj() throws Exception {
        String firstResponse = mockMvc.perform(post("/api/access/organizations")
                        .with(externalAdminTenantA())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Delga",
                                  "cnpj": "45.723.174/0001-10"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String firstOrganizationId = objectMapper.readTree(firstResponse).get("id").asText();

        String secondResponse = mockMvc.perform(post("/api/access/organizations")
                        .with(externalAdminTenantA())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Delga Alternative Name",
                                  "cnpj": "45.723.174/0001-10"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(firstOrganizationId))
                .andExpect(jsonPath("$.cnpj").value("45723174000110"))
                .andExpect(jsonPath("$.reused").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String secondOrganizationId = objectMapper.readTree(secondResponse).get("id").asText();
        org.assertj.core.api.Assertions.assertThat(secondOrganizationId).isEqualTo(firstOrganizationId);
    }

    @Test
    void shouldReturnStableErrorCodeWhenLocalOrganizationCodeAlreadyExistsForSameSource() throws Exception {
        mockMvc.perform(post("/api/access/organizations")
                        .with(externalAdminTenantA())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Customer One",
                                  "cnpj": "11.222.333/0001-81",
                                  "localOrganizationCode": "SHARED-CODE"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/access/organizations")
                        .with(externalAdminTenantA())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Customer Two",
                                  "cnpj": "22.333.444/0001-81",
                                  "localOrganizationCode": "SHARED-CODE"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.code").value("ORGANIZATION_RELATIONSHIP_LOCAL_CODE_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.details.field").value("localOrganizationCode"))
                .andExpect(jsonPath("$.details.value").value("SHARED-CODE"));
    }

    @Test
    void shouldPurgeOrganizationSubtreeEvenWhenRelationshipsAreInactive() throws Exception {
        String relationshipResponse = mockMvc.perform(post("/api/access/organizations/tenant-b/relationships")
                        .with(internalAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetOrganizationId": "tenant-a",
                                  "relationshipType": "PARTNER"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String relationshipId = objectMapper.readTree(relationshipResponse).get("id").asText();

        mockMvc.perform(delete("/api/access/organizations/tenant-b/relationships/" + relationshipId)
                        .with(internalAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));

        mockMvc.perform(post("/api/access/organizations/tenant-a/purge-subtree")
                        .with(internalSupport())
                        .param("supportOverride", "true")
                        .param("justification", "Cleanup after offboarding"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organizationId").value("tenant-a"))
                .andExpect(jsonPath("$.action").value("PURGE"))
                .andExpect(jsonPath("$.status").value("OK"));

        mockMvc.perform(get("/api/access/organizations")
                        .with(internalAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", not(hasItem("tenant-a"))));

        org.assertj.core.api.Assertions.assertThat(auditTrailService.findAll())
                .filteredOn(event -> "ORGANIZATION_PURGE_SUBTREE".equals(event.eventType()))
                .extracting(event -> event.targetTenantId())
                .contains("TEN-tenant-a");
    }

    @Test
    void shouldUpdateRelationshipLocalOrganizationCode() throws Exception {
        String relationshipResponse = mockMvc.perform(post("/api/access/organizations/tenant-b/relationships")
                        .with(internalAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetOrganizationId": "tenant-a",
                                  "relationshipType": "PARTNER",
                                  "localOrganizationCode": "TENANT-A-PARTNER"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.localOrganizationCode").value("TENANT-A-PARTNER"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String relationshipId = objectMapper.readTree(relationshipResponse).get("id").asText();

        mockMvc.perform(put("/api/access/organizations/tenant-b/relationships/" + relationshipId)
                        .with(internalAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "localOrganizationCode": "TENANT-A-PARTNER-UPD"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(relationshipId))
                .andExpect(jsonPath("$.localOrganizationCode").value("TENANT-A-PARTNER-UPD"));
    }

    @Test
    void shouldPurgeOrganizationSubtreeAndRemoveMembershipsThatReferenceIt() throws Exception {
        String createUserResponse = mockMvc.perform(post("/api/access/users")
                        .with(internalAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "Cross Tenant User",
                                  "email": "cross.tenant.user@oryzem.com",
                                  "organizationId": "tenant-b",
                                  "roles": ["MEMBER"]
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String userId = objectMapper.readTree(createUserResponse).get("id").asText();

        mockMvc.perform(post("/api/access/users/" + userId + "/memberships")
                        .with(internalAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantId": "TEN-tenant-a",
                                  "organizationId": "tenant-a",
                                  "roles": ["MEMBER"],
                                  "defaultMembership": false,
                                  "status": "ACTIVE"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/access/organizations/tenant-a/purge-subtree")
                        .with(internalSupport())
                        .param("supportOverride", "true")
                        .param("justification", "Cleanup memberships before purge"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organizationId").value("tenant-a"))
                .andExpect(jsonPath("$.status").value("OK"));

        mockMvc.perform(get("/api/access/users/" + userId + "/memberships")
                        .with(internalAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].organizationId", hasItem("tenant-b")))
                .andExpect(jsonPath("$[*].organizationId", not(hasItem("tenant-a"))));
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

    private org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor internalSupport() {
        return jwt().jwt(jwt -> jwt
                        .claim("sub", "support@oryzem.com-sub")
                        .claim("cognito:username", "support@oryzem.com")
                        .claim("email", "support@oryzem.com")
                        .claim("token_use", "access"))
                .authorities(new SimpleGrantedAuthority("ROLE_SUPPORT"));
    }
}
