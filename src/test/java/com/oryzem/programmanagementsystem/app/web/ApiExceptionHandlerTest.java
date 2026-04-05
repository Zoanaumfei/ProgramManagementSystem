package com.oryzem.programmanagementsystem.app.web;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

    @Test
    void shouldReturnSpecificConflictForMembershipOrganizationConstraint() throws Exception {
        mockMvc.perform(put("/api/access/users/USR-123/memberships/MBR-123")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.code").value("MEMBERSHIP_ORGANIZATION_REFERENCE_INVALID"))
                .andExpect(jsonPath("$.message").value(
                        "Nao foi possivel salvar o membership porque a organizacao informada nao existe ou nao esta mais disponivel."))
                .andExpect(jsonPath("$.hint").value("Revise a organizacao selecionada e tente novamente."));
    }

    @Test
    void shouldReturnSpecificConflictForMembershipRoleConstraint() throws Exception {
        mockMvc.perform(put("/api/access/users/USR-123/memberships/MBR-ROLE")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.code").value("MEMBERSHIP_ROLE_REFERENCE_INVALID"))
                .andExpect(jsonPath("$.message").value(
                        "Nao foi possivel salvar o membership porque um ou mais perfis informados sao invalidos."))
                .andExpect(jsonPath("$.hint").value("Revise os perfis selecionados e tente novamente."));
    }

    @Test
    void shouldReturnSpecificConflictForDuplicateMembershipRoleConstraint() throws Exception {
        mockMvc.perform(put("/api/access/users/USR-123/memberships/MBR-DUPLICATE-ROLE")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.code").value("MEMBERSHIP_ROLE_DUPLICATE"))
                .andExpect(jsonPath("$.message").value(
                        "Nao foi possivel salvar o membership porque o mesmo perfil foi associado mais de uma vez a este vinculo."))
                .andExpect(jsonPath("$.hint").value(
                        "Tente novamente. Se o problema persistir, revise os perfis atuais do membership antes de salvar."));
    }

    @Test
    void shouldKeepGenericConflictForUnknownIntegrityViolations() throws Exception {
        mockMvc.perform(put("/api/access/users/USR-123/memberships/MBR-GENERIC")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.code").value("DATA_INTEGRITY_VIOLATION"))
                .andExpect(jsonPath("$.message").value(
                        "Nao foi possivel concluir a operacao porque ainda existem dados relacionados a este registro."));
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

        @org.springframework.web.bind.annotation.PutMapping("/api/access/users/USR-123/memberships/MBR-123")
        String membershipOrganizationBoom() {
            throw new DataIntegrityViolationException(
                    "Membership update failed",
                    new RuntimeException(
                            "ERROR: insert or update on table \"user_membership\" violates foreign key constraint "
                                    + "\"fk_user_membership_organization\""));
        }

        @org.springframework.web.bind.annotation.PutMapping("/api/access/users/USR-123/memberships/MBR-ROLE")
        String membershipRoleBoom() {
            throw new DataIntegrityViolationException(
                    "Membership role update failed",
                    new RuntimeException(
                            "ERROR: insert or update on table \"membership_role\" violates foreign key constraint "
                                    + "\"fk_membership_role_role\""));
        }

        @org.springframework.web.bind.annotation.PutMapping("/api/access/users/USR-123/memberships/MBR-DUPLICATE-ROLE")
        String membershipDuplicateRoleBoom() {
            throw new DataIntegrityViolationException(
                    "Membership role duplicate failed",
                    new RuntimeException(
                            "ERROR: duplicate key value violates unique constraint "
                                    + "\"uq_membership_role_membership_code\""));
        }

        @org.springframework.web.bind.annotation.PutMapping("/api/access/users/USR-123/memberships/MBR-GENERIC")
        String membershipGenericBoom() {
            throw new DataIntegrityViolationException("Generic integrity failure");
        }
    }
}

