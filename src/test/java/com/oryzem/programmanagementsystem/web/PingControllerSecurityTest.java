package com.oryzem.programmanagementsystem.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PingControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void publicEndpointShouldBeAccessibleWithoutToken() throws Exception {
        mockMvc.perform(get("/public/ping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("public-ok"));
    }

    @Test
    void actuatorHealthShouldBeAccessibleWithoutToken() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void apiEndpointShouldReturnUnauthorizedWithoutToken() throws Exception {
        mockMvc.perform(get("/api/ping"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void apiEndpointShouldAllowAuthenticatedToken() throws Exception {
        mockMvc.perform(get("/api/ping")
                        .with(jwt().jwt(jwt -> jwt
                                        .claim("sub", "user-123")
                                        .claim("cognito:username", "alice"))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("api-ok"))
                .andExpect(jsonPath("$.principal").value("user-123"));
    }

    @Test
    void adminEndpointShouldReturnForbiddenWithoutAdminRole() throws Exception {
        mockMvc.perform(get("/api/admin/ping")
                        .with(jwt().jwt(jwt -> jwt
                                        .claim("sub", "user-123")
                                        .claim("cognito:username", "alice"))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    void adminEndpointShouldAllowAdminRole() throws Exception {
        mockMvc.perform(get("/api/admin/ping")
                        .with(jwt().jwt(jwt -> jwt
                                        .claim("sub", "admin-123")
                                        .claim("cognito:username", "admin"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("admin-ok"))
                .andExpect(jsonPath("$.principal").value("admin-123"));
    }

    @Test
    void corsShouldAllowConfiguredOrigin() throws Exception {
        mockMvc.perform(get("/public/ping")
                        .header("Origin", "http://localhost:3000"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
    }

    @Test
    void corsPreflightShouldAllowConfiguredOrigin() throws Exception {
        mockMvc.perform(options("/api/ping")
                        .header("Origin", "https://oryzem.com")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://oryzem.com"));
    }
}
