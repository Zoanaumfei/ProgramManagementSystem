package com.oryzem.programmanagementsystem.platform.access;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oryzem.programmanagementsystem.app.bootstrap.BootstrapDataService;
import com.oryzem.programmanagementsystem.platform.audit.AuditTrailService;
import com.oryzem.programmanagementsystem.platform.users.domain.UserRepository;
import com.oryzem.programmanagementsystem.platform.users.domain.UserStatus;
import org.assertj.core.api.Assertions;
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
class CoreSaasHardeningIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BootstrapDataService bootstrapDataService;

    @Autowired
    private AuditTrailService auditTrailService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TenantRateLimitingFilter tenantRateLimitingFilter;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        bootstrapDataService.reset();
        auditTrailService.clear();
        tenantRateLimitingFilter.clearCounters();
    }

    @Test
    void crossTenantReadAndWriteShouldBeDenied() throws Exception {
        mockMvc.perform(get("/api/access/users")
                        .param("organizationId", "tenant-b")
                        .with(jwtFor("admin.a@tenant.com", "ROLE_ADMIN")))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/access/users/USR-EXT-B-MEM-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "Cross Tenant Update",
                                  "email": "cross.tenant@tenant-b.com"
                                }
                                """)
                        .with(jwtFor("admin.a@tenant.com", "ROLE_ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void multiMembershipContextSwitchShouldAllowOwnedMembershipAndRejectBypassHint() throws Exception {
        String response = mockMvc.perform(post("/api/access/users/USR-EXT-A-MEM-001/memberships")
                        .with(jwtFor("admin@oryzem.com", "ROLE_ADMIN"))
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
        String alternateMembershipId = objectMapper.readTree(response).get("id").asText();

        mockMvc.perform(get("/api/auth/me")
                        .header("X-Access-Context", alternateMembershipId)
                        .with(jwtFor("member.a@tenant.com", "ROLE_MEMBER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.membershipId").value(alternateMembershipId))
                .andExpect(jsonPath("$.activeTenantId").value("TEN-tenant-b"));

        mockMvc.perform(post("/api/access/context/activate")
                        .with(jwtFor("member.a@tenant.com", "ROLE_MEMBER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "membershipId": "%s",
                                  "makeDefault": true
                                }
                                """.formatted(alternateMembershipId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.membershipId").value(alternateMembershipId));

        mockMvc.perform(get("/api/auth/me")
                        .header("X-Access-Context", "MBR-USR-EXT-B-MEM-001")
                        .with(jwtFor("member.a@tenant.com", "ROLE_MEMBER")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Requested access context is not available to the authenticated user."));
    }

    @Test
    void roleEscalationAttemptShouldBeDeniedForSupportActor() throws Exception {
        String createUserResponse = mockMvc.perform(post("/api/access/users")
                        .with(jwtFor("admin.a@tenant.com", "ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "Escalation Target",
                                  "email": "escalation.target@tenant.com",
                                  "organizationId": "tenant-a",
                                  "roles": ["MEMBER"]
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String userId = objectMapper.readTree(createUserResponse).get("id").asText();

        mockMvc.perform(put("/api/access/users/" + userId + "/memberships/MBR-" + userId)
                        .with(jwtFor("support.a@tenant.com", "ROLE_SUPPORT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantId": "TEN-tenant-a",
                                  "organizationId": "tenant-a",
                                  "roles": ["ADMIN"],
                                  "defaultMembership": true,
                                  "status": "ACTIVE"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void organizationScopedListingShouldNotLeakSiblingOrTenantUsers() throws Exception {
        String createOrganizationResponse = mockMvc.perform(post("/api/access/organizations")
                        .with(jwtFor("admin.a@tenant.com", "ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                      {
                        "name": "Tenant A Subsidiary",
                        "cnpj": "55.666.777/0001-81"
                      }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String organizationId = objectMapper.readTree(createOrganizationResponse).get("id").asText();

        String createUserResponse = mockMvc.perform(post("/api/access/users")
                        .with(jwtFor("admin.a@tenant.com", "ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "Subsidiary User",
                                  "email": "subsidiary.user@tenant.com",
                                  "organizationId": "%s",
                                  "roles": ["ADMIN"]
                                }
                                """.formatted(organizationId)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String userId = objectMapper.readTree(createUserResponse).get("id").asText();

        mockMvc.perform(get("/api/access/users")
                        .param("organizationId", organizationId)
                        .with(jwtFor("admin.a@tenant.com", "ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[*].id", hasItem(userId)))
                .andExpect(jsonPath("$[*].id", not(hasItem("USR-EXT-A-MEM-001"))));
    }

    @Test
    void organizationOffboardingShouldRevokeAccessAndRecordAudit() throws Exception {
        mockMvc.perform(delete("/api/access/organizations/tenant-b")
                        .with(jwtFor("admin@oryzem.com", "ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("tenant-b"))
                .andExpect(jsonPath("$.status").value("INACTIVE"));

        Assertions.assertThat(userRepository.findById("USR-EXT-B-MEM-001").orElseThrow().status())
                .isEqualTo(UserStatus.INACTIVE);
        Assertions.assertThat(auditTrailService.findAll())
                .extracting(event -> event.eventType())
                .contains("ORGANIZATION_OFFBOARD");
    }

    @Test
    void membershipOffboardingShouldAuditCriticalOperation() throws Exception {
        mockMvc.perform(delete("/api/access/users/USR-EXT-B-MEM-001/memberships/MBR-USR-EXT-B-MEM-001")
                        .with(jwtFor("admin@oryzem.com", "ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("MBR-USR-EXT-B-MEM-001"))
                .andExpect(jsonPath("$.status").value("INACTIVE"));

        Assertions.assertThat(userRepository.findById("USR-EXT-B-MEM-001").orElseThrow().status())
                .isEqualTo(UserStatus.INACTIVE);
        Assertions.assertThat(auditTrailService.findAll())
                .extracting(event -> event.eventType())
                .contains("MEMBERSHIP_OFFBOARD");
    }

    private org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtFor(
            String username,
            String authority) {
        return jwt().jwt(jwt -> jwt
                        .claim("sub", username + "-sub")
                        .claim("cognito:username", username)
                        .claim("email", username)
                        .claim("token_use", "access"))
                .authorities(new SimpleGrantedAuthority(authority));
    }
}
