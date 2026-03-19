package com.oryzem.programmanagementsystem.users;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oryzem.programmanagementsystem.audit.AuditTrailService;
import com.oryzem.programmanagementsystem.bootstrap.BootstrapDataService;
import org.assertj.core.api.Assertions;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserManagementControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private AuditTrailService auditTrailService;

    @Autowired
    private BootstrapDataService bootstrapDataService;

    @Autowired
    private UserIdentityGateway userIdentityGateway;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        bootstrapDataService.reset();
        auditTrailService.clear();
        if (userIdentityGateway instanceof StubUserIdentityGateway stubUserIdentityGateway) {
            stubUserIdentityGateway.clear();
        }
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
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].organizationId").value("tenant-a"))
                .andExpect(jsonPath("$[0].organizationName").value("Tenant A"))
                .andExpect(jsonPath("$[1].organizationId").value("tenant-a"))
                .andExpect(jsonPath("$[1].organizationName").value("Tenant A"))
                .andExpect(jsonPath("$[2].organizationId").value("tenant-a"))
                .andExpect(jsonPath("$[2].organizationName").value("Tenant A"));
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
                                  "organizationId": "tenant-a"
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
                .andExpect(jsonPath("$.organizationId").value("tenant-a"))
                .andExpect(jsonPath("$.organizationName").value("Tenant A"))
                .andExpect(jsonPath("$.status").value("INVITED"));
    }

    @Test
    void createUserShouldProvisionIdentityAndWriteAudit() throws Exception {
        String response = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "Identity Member",
                                  "email": "identity.member@tenant.com",
                                  "role": "MEMBER",
                                  "organizationId": "tenant-a"
                                }
                                """)
                        .with(jwt().jwt(jwt -> jwt
                                        .claim("sub", "manager-123")
                                        .claim("cognito:username", "manager")
                                        .claim("tenant_id", "tenant-a")
                                        .claim("tenant_type", "EXTERNAL"))
                                .authorities(new SimpleGrantedAuthority("ROLE_MANAGER"))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String createdUserId = objectMapper.readTree(response).get("id").asText();

        StubUserIdentityGateway stubUserIdentityGateway = (StubUserIdentityGateway) userIdentityGateway;
        Assertions.assertThat(stubUserIdentityGateway.operations())
                .extracting(StubUserIdentityOperation::action, StubUserIdentityOperation::identityUsername)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("CREATE", "identity.member@tenant.com"));

        Assertions.assertThat(auditTrailService.findAll())
                .extracting(event -> event.eventType(), event -> event.targetResourceId())
                .contains(org.assertj.core.groups.Tuple.tuple("USER_CREATE", createdUserId));
    }

    @Test
    void managerShouldUpdateMemberInOwnTenant() throws Exception {
        mockMvc.perform(put("/api/users/USR-EXT-A-MEM-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "Tenant A Member Updated",
                                  "email": "member.a.updated@tenant.com",
                                  "role": "MEMBER",
                                  "organizationId": "tenant-a"
                                }
                                """)
                        .with(jwt().jwt(jwt -> jwt
                                        .claim("sub", "manager-123")
                                        .claim("cognito:username", "manager")
                                        .claim("tenant_id", "tenant-a")
                                        .claim("tenant_type", "EXTERNAL"))
                                .authorities(new SimpleGrantedAuthority("ROLE_MANAGER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("USR-EXT-A-MEM-001"))
                .andExpect(jsonPath("$.displayName").value("Tenant A Member Updated"))
                .andExpect(jsonPath("$.email").value("member.a.updated@tenant.com"))
                .andExpect(jsonPath("$.organizationId").value("tenant-a"))
                .andExpect(jsonPath("$.organizationName").value("Tenant A"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void updateUserShouldSyncIdentityAndWriteAudit() throws Exception {
        mockMvc.perform(put("/api/users/USR-EXT-A-MEM-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "Tenant A Member Updated",
                                  "email": "member.a.updated@tenant.com",
                                  "role": "MEMBER",
                                  "organizationId": "tenant-a"
                                }
                                """)
                        .with(jwt().jwt(jwt -> jwt
                                        .claim("sub", "manager-123")
                                        .claim("cognito:username", "manager")
                                        .claim("tenant_id", "tenant-a")
                                        .claim("tenant_type", "EXTERNAL"))
                                .authorities(new SimpleGrantedAuthority("ROLE_MANAGER"))))
                .andExpect(status().isOk());

        StubUserIdentityGateway stubUserIdentityGateway = (StubUserIdentityGateway) userIdentityGateway;
        Assertions.assertThat(stubUserIdentityGateway.operations())
                .extracting(StubUserIdentityOperation::action, StubUserIdentityOperation::email)
                .contains(org.assertj.core.groups.Tuple.tuple("UPDATE", "member.a.updated@tenant.com"));

        Assertions.assertThat(auditTrailService.findAll())
                .extracting(event -> event.eventType(), event -> event.targetResourceId())
                .contains(org.assertj.core.groups.Tuple.tuple("USER_UPDATE", "USR-EXT-A-MEM-001"));
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
                                  "organizationId": "tenant-a"
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
    void shouldRequireFirstAdminBeforeCreatingNonAdminUsersInNewOrganization() throws Exception {
        String organizationId = createOrganization("New Supplier", "SUP-NEW");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "New Member",
                                  "email": "new.member@supplier.com",
                                  "role": "MEMBER",
                                  "organizationId": "%s"
                                }
                                """.formatted(organizationId))
                        .with(jwt().jwt(jwt -> jwt
                                        .claim("sub", "admin-123")
                                        .claim("cognito:username", "admin")
                                        .claim("tenant_id", "internal-core")
                                        .claim("tenant_type", "INTERNAL"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Organization is incomplete and requires a first ADMIN before non-admin users can be created or updated."));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "New Admin",
                                  "email": "new.admin@supplier.com",
                                  "role": "ADMIN",
                                  "organizationId": "%s"
                                }
                                """.formatted(organizationId))
                        .with(jwt().jwt(jwt -> jwt
                                        .claim("sub", "admin-123")
                                        .claim("cognito:username", "admin")
                                        .claim("tenant_id", "internal-core")
                                        .claim("tenant_type", "INTERNAL"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("ADMIN"));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "New Member",
                                  "email": "new.member@supplier.com",
                                  "role": "MEMBER",
                                  "organizationId": "%s"
                                }
                                """.formatted(organizationId))
                        .with(jwt().jwt(jwt -> jwt
                                        .claim("sub", "admin-123")
                                        .claim("cognito:username", "admin")
                                        .claim("tenant_id", "internal-core")
                                        .claim("tenant_type", "INTERNAL"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("MEMBER"));
    }

    @Test
    void createUserShouldRejectUnknownOrganization() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "Ghost Member",
                                  "email": "ghost.member@tenant.com",
                                  "role": "MEMBER",
                                  "organizationId": "tenant-z"
                                }
                                """)
                        .with(jwt().jwt(jwt -> jwt
                                        .claim("sub", "manager-123")
                                        .claim("cognito:username", "manager")
                                        .claim("tenant_id", "tenant-a")
                                        .claim("tenant_type", "EXTERNAL"))
                                .authorities(new SimpleGrantedAuthority("ROLE_MANAGER"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Organization not found: tenant-z"));
    }

    @Test
    void managerShouldNotUpdateAdminRole() throws Exception {
        mockMvc.perform(put("/api/users/USR-EXT-A-MEM-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "Escalated Member",
                                  "email": "member.a@tenant.com",
                                  "role": "ADMIN",
                                  "organizationId": "tenant-a"
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
    void shouldNotChangeOrganizationForActiveUser() throws Exception {
        mockMvc.perform(put("/api/users/USR-EXT-A-MEM-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "Tenant A Member Updated",
                                  "email": "member.a@tenant.com",
                                  "role": "MEMBER",
                                  "organizationId": "tenant-b"
                                }
                                """)
                        .with(jwt().jwt(jwt -> jwt
                                        .claim("sub", "admin-123")
                                        .claim("cognito:username", "admin")
                                        .claim("tenant_id", "internal-core")
                                        .claim("tenant_type", "INTERNAL"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Active users cannot change organization. Inactivate and recreate the user or update while still invited."));
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

        mockMvc.perform(get("/api/users")
                        .with(jwt().jwt(jwt -> jwt
                                        .claim("sub", "admin-123")
                                        .claim("cognito:username", "admin")
                                        .claim("tenant_id", "internal-core")
                                        .claim("tenant_type", "INTERNAL"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == 'USR-ADMIN-002')].status").value(org.hamcrest.Matchers.contains("INACTIVE")));
    }

    @Test
    void deleteUserShouldDisableIdentityAndWriteAudit() throws Exception {
        mockMvc.perform(delete("/api/users/USR-EXT-B-MEM-001")
                        .with(jwt().jwt(jwt -> jwt
                                        .claim("sub", "admin-123")
                                        .claim("cognito:username", "admin")
                                        .claim("tenant_id", "internal-core")
                                        .claim("tenant_type", "INTERNAL"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNoContent());

        StubUserIdentityGateway stubUserIdentityGateway = (StubUserIdentityGateway) userIdentityGateway;
        Assertions.assertThat(stubUserIdentityGateway.operations())
                .extracting(StubUserIdentityOperation::action, StubUserIdentityOperation::identityUsername)
                .contains(org.assertj.core.groups.Tuple.tuple("DISABLE", "member.b@tenant.com"));

        Assertions.assertThat(auditTrailService.findAll())
                .extracting(event -> event.eventType(), event -> event.targetResourceId())
                .contains(org.assertj.core.groups.Tuple.tuple("USER_INACTIVATE", "USR-EXT-B-MEM-001"));
    }

    @Test
    void supportShouldPurgeInactiveUserWhenIdentityIsAlreadyMissing() throws Exception {
        mockMvc.perform(delete("/api/users/USR-EXT-B-MEM-001")
                        .with(jwt().jwt(jwt -> jwt
                                        .claim("sub", "admin-123")
                                        .claim("cognito:username", "admin")
                                        .claim("tenant_id", "internal-core")
                                        .claim("tenant_type", "INTERNAL"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/users/USR-EXT-B-MEM-001/purge")
                        .param("supportOverride", "true")
                        .param("justification", "Cleanup of orphaned user after manual Cognito removal")
                        .with(jwt().jwt(jwt -> jwt
                                        .claim("sub", "support-123")
                                        .claim("cognito:username", "support")
                                        .claim("tenant_id", "internal-core")
                                        .claim("tenant_type", "INTERNAL"))
                                .authorities(new SimpleGrantedAuthority("ROLE_SUPPORT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.action").value("PURGE"))
                .andExpect(jsonPath("$.status").value("OK"));

        Assertions.assertThat(userRepository.findById("USR-EXT-B-MEM-001")).isEmpty();
        Assertions.assertThat(auditTrailService.findAll())
                .extracting(event -> event.eventType(), event -> event.targetResourceId())
                .contains(org.assertj.core.groups.Tuple.tuple("USER_PURGE", "USR-EXT-B-MEM-001"));
    }

    @Test
    void supportShouldNotPurgeActiveUser() throws Exception {
        mockMvc.perform(post("/api/users/USR-EXT-B-MEM-001/purge")
                        .param("supportOverride", "true")
                        .param("justification", "Cleanup of orphaned user after manual Cognito removal")
                        .with(jwt().jwt(jwt -> jwt
                                        .claim("sub", "support-123")
                                        .claim("cognito:username", "support")
                                        .claim("tenant_id", "internal-core")
                                        .claim("tenant_type", "INTERNAL"))
                                .authorities(new SimpleGrantedAuthority("ROLE_SUPPORT"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only inactive users can be purged."));
    }

    @Test
    void supportShouldNotPurgeWithoutExplicitOverride() throws Exception {
        mockMvc.perform(delete("/api/users/USR-EXT-B-MEM-001")
                        .with(jwt().jwt(jwt -> jwt
                                        .claim("sub", "admin-123")
                                        .claim("cognito:username", "admin")
                                        .claim("tenant_id", "internal-core")
                                        .claim("tenant_type", "INTERNAL"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/users/USR-EXT-B-MEM-001/purge")
                        .param("justification", "Cleanup of orphaned user after manual Cognito removal")
                        .with(jwt().jwt(jwt -> jwt
                                        .claim("sub", "support-123")
                                        .claim("cognito:username", "support")
                                        .claim("tenant_id", "internal-core")
                                        .claim("tenant_type", "INTERNAL"))
                                .authorities(new SimpleGrantedAuthority("ROLE_SUPPORT"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("User purge requires supportOverride=true."));
    }

    @Test
    void supportShouldNotPurgeWhenIdentityStillExists() throws Exception {
        mockMvc.perform(delete("/api/users/USR-EXT-B-MEM-001")
                        .with(jwt().jwt(jwt -> jwt
                                        .claim("sub", "admin-123")
                                        .claim("cognito:username", "admin")
                                        .claim("tenant_id", "internal-core")
                                        .claim("tenant_type", "INTERNAL"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNoContent());

        StubUserIdentityGateway stubUserIdentityGateway = (StubUserIdentityGateway) userIdentityGateway;
        stubUserIdentityGateway.markIdentityPresent("member.b@tenant.com");

        mockMvc.perform(post("/api/users/USR-EXT-B-MEM-001/purge")
                        .param("supportOverride", "true")
                        .param("justification", "Cleanup of orphaned user after manual Cognito removal")
                        .with(jwt().jwt(jwt -> jwt
                                        .claim("sub", "support-123")
                                        .claim("cognito:username", "support")
                                        .claim("tenant_id", "internal-core")
                                        .claim("tenant_type", "INTERNAL"))
                                .authorities(new SimpleGrantedAuthority("ROLE_SUPPORT"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "User purge is allowed only when the identity is already absent from Cognito."));
    }

    @Test
    void shouldNotResetAccessForInactiveUser() throws Exception {
        mockMvc.perform(delete("/api/users/USR-EXT-B-MEM-001")
                        .with(jwt().jwt(jwt -> jwt
                                        .claim("sub", "admin-123")
                                        .claim("cognito:username", "admin")
                                        .claim("tenant_id", "internal-core")
                                        .claim("tenant_type", "INTERNAL"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/users/USR-EXT-B-MEM-001/reset-access")
                        .with(jwt().jwt(jwt -> jwt
                                        .claim("sub", "admin-123")
                                        .claim("cognito:username", "admin")
                                        .claim("tenant_id", "internal-core")
                                        .claim("tenant_type", "INTERNAL"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Inactive users cannot receive sensitive access actions."));
    }

    @Test
    void shouldNotResetAccessForInvitedUser() throws Exception {
        String response = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "Invited Member",
                                  "email": "invited.reset@tenant.com",
                                  "role": "MEMBER",
                                  "organizationId": "tenant-a"
                                }
                                """)
                        .with(jwt().jwt(jwt -> jwt
                                        .claim("sub", "manager-123")
                                        .claim("cognito:username", "manager")
                                        .claim("tenant_id", "tenant-a")
                                        .claim("tenant_type", "EXTERNAL"))
                                .authorities(new SimpleGrantedAuthority("ROLE_MANAGER"))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String createdUserId = objectMapper.readTree(response).get("id").asText();

        mockMvc.perform(post("/api/users/" + createdUserId + "/reset-access")
                        .with(jwt().jwt(jwt -> jwt
                                        .claim("sub", "manager-123")
                                        .claim("cognito:username", "manager")
                                        .claim("tenant_id", "tenant-a")
                                        .claim("tenant_type", "EXTERNAL"))
                                .authorities(new SimpleGrantedAuthority("ROLE_MANAGER"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only active users can receive access reset."));
    }

    @Test
    void invitedUserShouldBecomeActiveAfterFirstAuthenticatedRequest() throws Exception {
        String response = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "Invited Member",
                                  "email": "invited.member@tenant.com",
                                  "role": "MEMBER",
                                  "organizationId": "tenant-a"
                                }
                                """)
                        .with(jwt().jwt(jwt -> jwt
                                        .claim("sub", "manager-123")
                                        .claim("cognito:username", "manager")
                                        .claim("tenant_id", "tenant-a")
                                        .claim("tenant_type", "EXTERNAL"))
                                .authorities(new SimpleGrantedAuthority("ROLE_MANAGER"))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String createdUserId = objectMapper.readTree(response).get("id").asText();

        mockMvc.perform(get("/api/auth/me")
                        .with(jwt().jwt(jwt -> jwt
                                        .claim("sub", "invited-user-sub")
                                        .claim("cognito:username", "invited.member@tenant.com")
                                        .claim("email", "invited.member@tenant.com")
                                        .claim("tenant_id", "tenant-a")
                                        .claim("tenant_type", "EXTERNAL"))
                                .authorities(new SimpleGrantedAuthority("ROLE_MEMBER"))))
                .andExpect(status().isOk());

        ManagedUser activatedUser = userRepository.findById(createdUserId).orElseThrow();
        Assertions.assertThat(activatedUser.status()).isEqualTo(UserStatus.ACTIVE);
        Assertions.assertThat(activatedUser.identitySubject()).isEqualTo("invited-user-sub");
    }

    @Test
    void invitedUserShouldBecomeActiveWithCustomCognitoTenantClaims() throws Exception {
        String response = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "Invited Member Custom",
                                  "email": "invited.custom@tenant.com",
                                  "role": "MEMBER",
                                  "organizationId": "tenant-a"
                                }
                                """)
                        .with(jwt().jwt(jwt -> jwt
                                        .claim("sub", "manager-123")
                                        .claim("cognito:username", "manager")
                                        .claim("tenant_id", "tenant-a")
                                        .claim("tenant_type", "EXTERNAL"))
                                .authorities(new SimpleGrantedAuthority("ROLE_MANAGER"))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String createdUserId = objectMapper.readTree(response).get("id").asText();

        mockMvc.perform(get("/api/auth/me")
                        .with(jwt().jwt(jwt -> jwt
                                        .claim("sub", "invited-custom-sub")
                                        .claim("cognito:username", "invited.custom@tenant.com")
                                        .claim("email", "invited.custom@tenant.com")
                                        .claim("custom:tenant_id", "tenant-a")
                                        .claim("custom:tenant_type", "EXTERNAL")
                                        .claim("custom:user_status", "ACTIVE"))
                                .authorities(new SimpleGrantedAuthority("ROLE_MEMBER"))))
                .andExpect(status().isOk());

        ManagedUser activatedUser = userRepository.findById(createdUserId).orElseThrow();
        Assertions.assertThat(activatedUser.status()).isEqualTo(UserStatus.ACTIVE);
        Assertions.assertThat(activatedUser.identitySubject()).isEqualTo("invited-custom-sub");
    }

    @Test
    void invitedUserShouldBecomeActiveWithAccessTokenUsernameClaim() throws Exception {
        String response = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "Invited Member Access",
                                  "email": "invited.access@tenant.com",
                                  "role": "MEMBER",
                                  "organizationId": "tenant-a"
                                }
                                """)
                        .with(jwt().jwt(jwt -> jwt
                                        .claim("sub", "manager-123")
                                        .claim("cognito:username", "manager")
                                        .claim("tenant_id", "tenant-a")
                                        .claim("tenant_type", "EXTERNAL"))
                                .authorities(new SimpleGrantedAuthority("ROLE_MANAGER"))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String createdUserId = objectMapper.readTree(response).get("id").asText();

        mockMvc.perform(get("/api/auth/me")
                        .with(jwt().jwt(jwt -> jwt
                                        .claim("sub", "invited-access-sub")
                                        .claim("username", "invited.access@tenant.com")
                                        .claim("tenant_id", "tenant-a")
                                        .claim("tenant_type", "EXTERNAL")
                                        .claim("token_use", "access"))
                                .authorities(new SimpleGrantedAuthority("ROLE_MEMBER"))))
                .andExpect(status().isOk());

        ManagedUser activatedUser = userRepository.findById(createdUserId).orElseThrow();
        Assertions.assertThat(activatedUser.status()).isEqualTo(UserStatus.ACTIVE);
        Assertions.assertThat(activatedUser.identitySubject()).isEqualTo("invited-access-sub");
    }

    private String createOrganization(String name, String code) throws Exception {
        String response = mockMvc.perform(post("/api/portfolio/organizations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "code": "%s"
                                }
                                """.formatted(name, code))
                        .with(jwt().jwt(jwt -> jwt
                                        .claim("sub", "admin-123")
                                        .claim("cognito:username", "admin")
                                        .claim("tenant_id", "internal-core")
                                        .claim("tenant_type", "INTERNAL"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode json = objectMapper.readTree(response);
        return json.get("id").asText();
    }
}
