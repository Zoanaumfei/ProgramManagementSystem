package com.oryzem.programmanagementsystem.web;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void authConfigShouldBePublic() throws Exception {
        mockMvc.perform(get("/public/auth/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("aws-cognito"))
                .andExpect(jsonPath("$.issuerUri").isNotEmpty())
                .andExpect(jsonPath("$.appClientId").isNotEmpty());
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
}
