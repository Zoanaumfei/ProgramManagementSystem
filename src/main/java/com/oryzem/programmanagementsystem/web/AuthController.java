package com.oryzem.programmanagementsystem.web;

import com.oryzem.programmanagementsystem.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.authorization.AuthenticatedUserMapper;
import com.oryzem.programmanagementsystem.config.CognitoProperties;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class AuthController {

    private static final String GROUPS_CLAIM = "cognito:groups";
    private static final String USERNAME_CLAIM = "cognito:username";

    private final CognitoProperties cognitoProperties;
    private final AuthenticatedUserMapper authenticatedUserMapper;

    public AuthController(
            CognitoProperties cognitoProperties,
            AuthenticatedUserMapper authenticatedUserMapper) {
        this.cognitoProperties = cognitoProperties;
        this.authenticatedUserMapper = authenticatedUserMapper;
    }

    @GetMapping("/public/auth/config")
    public Map<String, Object> authConfig() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("provider", "aws-cognito");
        body.put("issuerUri", cognitoProperties.issuerUri());
        body.put("jwkSetUri", cognitoProperties.jwkSetUri());
        body.put("appClientId", cognitoProperties.appClientId());
        body.put("timestamp", Instant.now().toString());
        return body;
    }

    @GetMapping("/api/auth/me")
    public Map<String, Object> me(Authentication authentication) {
        JwtAuthenticationToken jwtAuthentication = (JwtAuthenticationToken) authentication;
        Jwt jwt = jwtAuthentication.getToken();
        AuthenticatedUser authenticatedUser = authenticatedUserMapper.from(authentication);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("subject", jwt.getSubject());
        body.put("username", firstNonBlank(jwt.getClaimAsString(USERNAME_CLAIM), jwtAuthentication.getName()));
        body.put("email", jwt.getClaimAsString("email"));
        body.put("tokenUse", jwt.getClaimAsString("token_use"));
        body.put("tenantId", authenticatedUser.tenantId());
        body.put("tenantType", authenticatedUser.tenantType() != null ? authenticatedUser.tenantType().name() : null);
        body.put("roles", authenticatedUser.roles().stream().map(Enum::name).sorted().toList());
        body.put("groups", defaultList(jwt.getClaimAsStringList(GROUPS_CLAIM)));
        body.put("scopes", scopes(jwt.getClaimAsString("scope")));
        body.put("authorities", jwtAuthentication.getAuthorities().stream()
                .map(Object::toString)
                .sorted()
                .toList());
        body.put("timestamp", Instant.now().toString());
        return body;
    }

    private String firstNonBlank(String firstValue, String fallbackValue) {
        return Stream.of(firstValue, fallbackValue)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse("unknown");
    }

    private List<String> defaultList(List<String> values) {
        return values == null ? List.of() : values;
    }

    private List<String> scopes(String scopeClaim) {
        if (scopeClaim == null || scopeClaim.isBlank()) {
            return List.of();
        }

        return Stream.of(scopeClaim.trim().split("\\s+"))
                .filter(scope -> !scope.isBlank())
                .toList();
    }
}
