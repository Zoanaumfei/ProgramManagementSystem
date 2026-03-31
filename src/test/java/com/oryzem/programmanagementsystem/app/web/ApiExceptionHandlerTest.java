package com.oryzem.programmanagementsystem.app.web;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest(classes = {
        com.oryzem.programmanagementsystem.app.ProgramManagementSystemApplication.class,
        ApiExceptionHandlerTest.TestConfig.class
})
@AutoConfigureMockMvc
class ApiExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnJsonForUnhandledExceptionsWithoutMaskingAsForbidden() throws Exception {
        mockMvc.perform(get("/api/test/boom")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("Unexpected server error."))
                .andExpect(jsonPath("$.path").value("/api/test/boom"))
                .andExpect(jsonPath("$.correlationId", not(blankOrNullString())));
    }

    @Test
    void shouldReturnJsonForMissingRoutesInsteadOfInternalServerError() throws Exception {
        mockMvc.perform(get("/api/test/missing")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.path").value("/api/test/missing"))
                .andExpect(jsonPath("$.correlationId", not(blankOrNullString())));
    }

    @Test
    void shouldReturnFriendlyConflictForPurgeBlockedByMembershipReferences() throws Exception {
        mockMvc.perform(post("/api/access/organizations/ORG-123/purge-subtree")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPPORT"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.code").value("ORGANIZATION_PURGE_BLOCKED_BY_MEMBERSHIPS"))
                .andExpect(jsonPath("$.message").value("Quase la: ainda existem vínculos de acesso ligados a esta organização, então não consegui concluir o purge."))
                .andExpect(jsonPath("$.hint").value("Remova ou revise os vínculos de acesso relacionados a esta organização antes de tentar o purge novamente."));
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        BoomController boomController() {
            return new BoomController();
        }
    }

    @RestController
    static class BoomController {

        @GetMapping("/api/test/boom")
        String boom() {
            throw new RuntimeException("boom");
        }

        @org.springframework.web.bind.annotation.PostMapping("/api/access/organizations/ORG-123/purge-subtree")
        String purgeBoom() {
            throw new DataIntegrityViolationException(
                    "ERROR: update or delete on table \"organization\" violates foreign key constraint "
                            + "\"fk_user_membership_organization\" on table \"user_membership\"");
        }
    }
}

