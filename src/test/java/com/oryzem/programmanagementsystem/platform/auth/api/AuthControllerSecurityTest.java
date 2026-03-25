package com.oryzem.programmanagementsystem.platform.auth.api;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = com.oryzem.programmanagementsystem.app.ProgramManagementSystemApplication.class)
@AutoConfigureMockMvc
class AuthControllerSecurityTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired
    private MockMvc mockMvc;

    @Test
    void authConfigShouldBePublic() throws Exception {
        mockMvc.perform(get("/public/auth/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("aws-cognito"))
                .andExpect(jsonPath("$.mode").value("custom-login-ready"))
                .andExpect(jsonPath("$.issuerUri").isNotEmpty())
                .andExpect(jsonPath("$.appClientId").isNotEmpty());
    }

    @Test
    void loginShouldAuthenticateWithStubGateway() throws Exception {
        mockMvc.perform(post("/public/auth/login")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "member@oryzem.com",
                                  "password": "Password123!"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AUTHENTICATED"))
                .andExpect(jsonPath("$.username").value("member@oryzem.com"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    void loginShouldExposeNewPasswordChallengeWhenRequired() throws Exception {
        mockMvc.perform(post("/public/auth/login")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "new-password.user@oryzem.com",
                                  "password": "Temp123!"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NEW_PASSWORD_REQUIRED"))
                .andExpect(jsonPath("$.challengeName").value("NEW_PASSWORD_REQUIRED"))
                .andExpect(jsonPath("$.session").isNotEmpty());
    }

    @Test
    void loginShouldExposePasswordResetRequiredStateWhenRequired() throws Exception {
        mockMvc.perform(post("/public/auth/login")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "reset-required.user@oryzem.com",
                                  "password": "Password123!"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PASSWORD_RESET_REQUIRED"))
                .andExpect(jsonPath("$.challengeName").value("PASSWORD_RESET_REQUIRED"));
    }

    @Test
    void shouldCompleteNewPasswordAndPasswordResetFlowsWithStubGateway() throws Exception {
        String challengeResponse = mockMvc.perform(post("/public/auth/login")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "new-password.finish@oryzem.com",
                                  "password": "Temp123!"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String session = requiredText(JSON.readTree(challengeResponse), "session");

        mockMvc.perform(post("/public/auth/login/new-password")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "new-password.finish@oryzem.com",
                                  "session": "%s",
                                  "newPassword": "Changed123!"
                                }
                                """.formatted(session)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AUTHENTICATED"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty());

        mockMvc.perform(post("/public/auth/password-reset/code")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "reset-required.user@oryzem.com"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CODE_SENT"))
                .andExpect(jsonPath("$.deliveryMedium").value("EMAIL"));

        mockMvc.perform(post("/public/auth/password-reset/confirm")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "reset-required.user@oryzem.com",
                                  "code": "654321",
                                  "newPassword": "Changed123!"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PASSWORD_RESET_CONFIRMED"));
    }

    @Test
    void refreshShouldIssueNewAccessTokenWithStubGateway() throws Exception {
        String authenticatedResponse = mockMvc.perform(post("/public/auth/login")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "refresh.user@oryzem.com",
                                  "password": "Password123!"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String refreshToken = requiredText(JSON.readTree(authenticatedResponse), "refreshToken");

        mockMvc.perform(post("/public/auth/refresh")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "refresh.user@oryzem.com",
                                  "refreshToken": "%s"
                                }
                                """.formatted(refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AUTHENTICATED"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void meShouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void meShouldExposeAuthenticatedUserContext() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .with(jwt().jwt(jwt -> jwt
                                        .claim("sub", "user-123")
                                        .claim("cognito:username", "alice")
                                        .claim("email", "alice@oryzem.com")
                                        .claim("email_verified", false)
                                        .claim("tenant_id", "tenant-a")
                                        .claim("tenant_type", "EXTERNAL")
                                        .claim("token_use", "access")
                                        .claim("scope", "openid profile")
                                        .claim("cognito:groups", List.of("admin", "program-managers")))
                                .authorities(
                                        new SimpleGrantedAuthority("ROLE_ADMIN"),
                                        new SimpleGrantedAuthority("ROLE_PROGRAM_MANAGERS"),
                                        new SimpleGrantedAuthority("SCOPE_openid"),
                                        new SimpleGrantedAuthority("SCOPE_profile"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject").value("user-123"))
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.email").value("alice@oryzem.com"))
                .andExpect(jsonPath("$.emailVerified").value(false))
                .andExpect(jsonPath("$.emailVerificationRequired").value(true))
                .andExpect(jsonPath("$.tokenUse").value("access"))
                .andExpect(jsonPath("$.tenantId").value("tenant-a"))
                .andExpect(jsonPath("$.tenantType").value("EXTERNAL"))
                .andExpect(jsonPath("$.roles[0]").value("ADMIN"))
                .andExpect(jsonPath("$.groups[0]").value("admin"))
                .andExpect(jsonPath("$.groups[1]").value("program-managers"))
                .andExpect(jsonPath("$.scopes[0]").value("openid"))
                .andExpect(jsonPath("$.scopes[1]").value("profile"))
                .andExpect(jsonPath("$.authorities[0]").value("ROLE_ADMIN"))
                .andExpect(jsonPath("$.authorities[3]").value("SCOPE_profile"));
    }

    @Test
    void meShouldSupportCustomCognitoTenantClaims() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .with(jwt().jwt(jwt -> jwt
                                        .claim("sub", "user-456")
                                        .claim("cognito:username", "bob")
                                        .claim("email", "bob@oryzem.com")
                                        .claim("custom:tenant_id", "tenant-b")
                                        .claim("custom:tenant_type", "EXTERNAL")
                                        .claim("custom:user_status", "ACTIVE")
                                        .claim("token_use", "id"))
                                .authorities(new SimpleGrantedAuthority("ROLE_MANAGER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject").value("user-456"))
                .andExpect(jsonPath("$.username").value("bob"))
                .andExpect(jsonPath("$.tenantId").value("tenant-b"))
                .andExpect(jsonPath("$.tenantType").value("EXTERNAL"))
                .andExpect(jsonPath("$.tenantIdClaim").value("tenant-b"))
                .andExpect(jsonPath("$.tenantTypeClaim").value("EXTERNAL"))
                .andExpect(jsonPath("$.userStatusClaim").value("ACTIVE"));
    }

    @Test
    void meShouldExposeAccessTokenUsernameClaimWhenCognitoUsernameIsMissing() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .with(jwt().jwt(jwt -> jwt
                                        .claim("sub", "user-789")
                                        .claim("username", "access-user")
                                        .claim("tenant_id", "internal-core")
                                        .claim("tenant_type", "INTERNAL")
                                        .claim("token_use", "access"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject").value("user-789"))
                .andExpect(jsonPath("$.username").value("access-user"))
                .andExpect(jsonPath("$.tenantId").value("internal-core"))
                .andExpect(jsonPath("$.tenantType").value("INTERNAL"));
    }

    @Test
    void shouldSendAndConfirmEmailVerificationCodeForAuthenticatedUser() throws Exception {
        mockMvc.perform(post("/api/auth/email-verification/code")
                        .with(jwt().jwt(jwt -> jwt
                                        .claim("sub", "user-verify-123")
                                        .claim("cognito:username", "verify.user")
                                        .claim("email", "verify.user@oryzem.com")
                                        .claim("email_verified", false)
                                        .claim("token_use", "access"))
                                .authorities(new SimpleGrantedAuthority("ROLE_MEMBER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CODE_SENT"))
                .andExpect(jsonPath("$.email").value("verify.user@oryzem.com"))
                .andExpect(jsonPath("$.emailVerified").value(false))
                .andExpect(jsonPath("$.emailVerificationRequired").value(true));

        mockMvc.perform(post("/api/auth/email-verification/confirm")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "123456"
                                }
                                """)
                        .with(jwt().jwt(jwt -> jwt
                                        .claim("sub", "user-verify-123")
                                        .claim("cognito:username", "verify.user")
                                        .claim("email", "verify.user@oryzem.com")
                                        .claim("email_verified", false)
                                        .claim("token_use", "access"))
                                .authorities(new SimpleGrantedAuthority("ROLE_MEMBER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VERIFIED"))
                .andExpect(jsonPath("$.emailVerified").value(true))
                .andExpect(jsonPath("$.emailVerificationRequired").value(false));
    }

    @Test
    void logoutShouldRequireAuthentication() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutShouldSignOutAuthenticatedUser() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .with(jwt().jwt(jwt -> jwt
                                        .claim("sub", "logout-user")
                                        .claim("cognito:username", "logout.user")
                                        .claim("token_use", "access"))
                                .authorities(new SimpleGrantedAuthority("ROLE_MEMBER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SIGNED_OUT"));
    }

    private static String requiredText(JsonNode response, String fieldName) {
        return response.required(fieldName)
                .stringValueOpt()
                .filter(value -> !value.isBlank())
                .orElseThrow(() -> new IllegalStateException(
                        "Expected non-empty text field '%s' in response.".formatted(fieldName)));
    }
}

