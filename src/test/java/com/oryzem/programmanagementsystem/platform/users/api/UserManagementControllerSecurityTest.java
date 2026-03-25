package com.oryzem.programmanagementsystem.platform.users.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oryzem.programmanagementsystem.app.bootstrap.BootstrapDataService;
import com.oryzem.programmanagementsystem.platform.audit.AuditTrailService;
import com.oryzem.programmanagementsystem.platform.users.domain.ManagedUser;
import com.oryzem.programmanagementsystem.platform.users.domain.UserIdentityGateway;
import com.oryzem.programmanagementsystem.platform.users.domain.UserRepository;
import com.oryzem.programmanagementsystem.platform.users.domain.UserStatus;
import com.oryzem.programmanagementsystem.platform.users.infrastructure.StubUserIdentityGateway;
import com.oryzem.programmanagementsystem.platform.users.infrastructure.StubUserIdentityOperation;
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

@SpringBootTest(classes = com.oryzem.programmanagementsystem.app.ProgramManagementSystemApplication.class)
@AutoConfigureMockMvc
class UserManagementControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BootstrapDataService bootstrapDataService;

    @Autowired
    private AuditTrailService auditTrailService;

    @Autowired
    private UserIdentityGateway userIdentityGateway;

    @Autowired
    private UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        bootstrapDataService.reset();
        auditTrailService.clear();
        if (userIdentityGateway instanceof StubUserIdentityGateway stubGateway) {
            stubGateway.clear();
        }
    }

    @Test
    void listUsersShouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/access/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void externalAdminShouldSeeOnlyOwnHierarchyUsersByDefault() throws Exception {
        mockMvc.perform(get("/api/access/users")
                        .with(jwtFor("admin.a@tenant.com", "ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].organizationId").value("tenant-a"))
                .andExpect(jsonPath("$[0].organizationName").value("Tenant A"));
    }

    @Test
    void managerShouldNotListUsers() throws Exception {
        mockMvc.perform(get("/api/access/users")
                        .with(jwtFor("manager.a@tenant.com", "ROLE_MANAGER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void externalAdminShouldCreateMemberInOwnTenant() throws Exception {
        mockMvc.perform(post("/api/access/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "New Member",
                                  "email": "new.member@tenant.com",
                                  "role": "MEMBER",
                                  "organizationId": "tenant-a"
                                }
                                """)
                        .with(jwtFor("admin.a@tenant.com", "ROLE_ADMIN")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/api/access/users/")))
                .andExpect(jsonPath("$.role").value("MEMBER"))
                .andExpect(jsonPath("$.organizationId").value("tenant-a"))
                .andExpect(jsonPath("$.status").value("INVITED"));

        StubUserIdentityGateway stubGateway = (StubUserIdentityGateway) userIdentityGateway;
        Assertions.assertThat(stubGateway.operations())
                .extracting(StubUserIdentityOperation::action, StubUserIdentityOperation::identityUsername)
                .contains(org.assertj.core.groups.Tuple.tuple("CREATE", "new.member@tenant.com"));
    }

    @Test
    void externalAdminShouldUpdateMemberInOwnTenant() throws Exception {
        mockMvc.perform(put("/api/access/users/USR-EXT-A-MEM-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "Tenant A Member Updated",
                                  "email": "member.a.updated@tenant.com",
                                  "role": "MEMBER",
                                  "organizationId": "tenant-a"
                                }
                                """)
                        .with(jwtFor("admin.a@tenant.com", "ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("USR-EXT-A-MEM-001"))
                .andExpect(jsonPath("$.email").value("member.a.updated@tenant.com"))
                .andExpect(jsonPath("$.organizationId").value("tenant-a"));
    }

    @Test
    void internalAdminShouldInactivateUser() throws Exception {
        mockMvc.perform(delete("/api/access/users/USR-EXT-B-MEM-001")
                        .with(jwtFor("admin@oryzem.com", "ROLE_ADMIN")))
                .andExpect(status().isNoContent());

        ManagedUser disabled = userRepository.findById("USR-EXT-B-MEM-001").orElseThrow();
        Assertions.assertThat(disabled.status()).isEqualTo(UserStatus.INACTIVE);
    }

    @Test
    void supportShouldPurgeInactiveUserWhenIdentityIsAlreadyMissing() throws Exception {
        mockMvc.perform(delete("/api/access/users/USR-EXT-B-MEM-001")
                        .with(jwtFor("admin@oryzem.com", "ROLE_ADMIN")))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/access/users/USR-EXT-B-MEM-001/purge")
                        .param("supportOverride", "true")
                        .param("justification", "Cleanup of orphaned user after manual Cognito removal")
                        .with(jwtFor("support@oryzem.com", "ROLE_SUPPORT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.action").value("PURGE"));

        Assertions.assertThat(userRepository.findById("USR-EXT-B-MEM-001")).isEmpty();
    }

    @Test
    void shouldRequireVerifiedEmailBeforeResetAccess() throws Exception {
        StubUserIdentityGateway stubGateway = (StubUserIdentityGateway) userIdentityGateway;
        stubGateway.markRecoveryChannelUnverified("member.b@tenant.com");

        mockMvc.perform(post("/api/access/users/USR-EXT-B-MEM-001/reset-access")
                        .with(jwtFor("admin@oryzem.com", "ROLE_ADMIN")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("The user must verify email before access reset can be used."));
    }

    @Test
    void invitedUserShouldBecomeActiveAfterFirstAuthenticatedRequest() throws Exception {
        String response = mockMvc.perform(post("/api/access/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "Invited Member",
                                  "email": "invited.member@tenant.com",
                                  "role": "MEMBER",
                                  "organizationId": "tenant-a"
                                }
                                """)
                        .with(jwtFor("admin.a@tenant.com", "ROLE_ADMIN")))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String createdUserId = objectMapper.readTree(response).get("id").asText();

        mockMvc.perform(get("/api/auth/me")
                        .with(jwtFor("invited.member@tenant.com", "ROLE_MEMBER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(createdUserId));

        ManagedUser activatedUser = userRepository.findById(createdUserId).orElseThrow();
        Assertions.assertThat(activatedUser.status()).isEqualTo(UserStatus.ACTIVE);
        Assertions.assertThat(activatedUser.identitySubject()).isEqualTo("invited.member@tenant.com-sub");
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
