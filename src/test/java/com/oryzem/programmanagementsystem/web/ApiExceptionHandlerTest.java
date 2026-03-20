package com.oryzem.programmanagementsystem.web;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest
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
    }
}
