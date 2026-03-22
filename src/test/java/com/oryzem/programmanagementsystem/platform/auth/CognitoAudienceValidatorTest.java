package com.oryzem.programmanagementsystem.platform.auth;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.assertj.core.api.Assertions.assertThat;

class CognitoAudienceValidatorTest {

    private final CognitoAudienceValidator validator = new CognitoAudienceValidator("rv7hk9nkugspb3i4p269sv828");

    @Test
    void shouldAcceptTokenWhenAudienceContainsConfiguredClientId() {
        Jwt jwt = baseJwtBuilder()
                .audience(List.of("rv7hk9nkugspb3i4p269sv828"))
                .build();

        assertThat(validator.validate(jwt).hasErrors()).isFalse();
    }

    @Test
    void shouldAcceptTokenWhenClientIdClaimMatchesConfiguredClientId() {
        Jwt jwt = baseJwtBuilder()
                .claim("client_id", "rv7hk9nkugspb3i4p269sv828")
                .build();

        assertThat(validator.validate(jwt).hasErrors()).isFalse();
    }

    @Test
    void shouldRejectTokenWhenAudienceAndClientIdDoNotMatch() {
        Jwt jwt = baseJwtBuilder()
                .audience(List.of("different-client"))
                .claim("client_id", "different-client")
                .build();

        assertThat(validator.validate(jwt).hasErrors()).isTrue();
    }

    private Jwt.Builder baseJwtBuilder() {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", "user-123")
                .issuer("https://cognito-idp.sa-east-1.amazonaws.com/sa-east-1_aA4I3tEmF")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300));
    }
}

