package com.oryzem.programmanagementsystem.authorization;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class AuthenticatedUserMapper {

    private static final String USERNAME_CLAIM = "cognito:username";
    private static final String TENANT_ID_CLAIM = "tenant_id";
    private static final String TENANT_TYPE_CLAIM = "tenant_type";

    public AuthenticatedUser from(Authentication authentication) {
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
            throw new IllegalArgumentException("Unsupported authentication type for authorization mapping.");
        }

        Jwt jwt = jwtAuthentication.getToken();
        Set<Role> roles = authentication.getAuthorities().stream()
                .map(authority -> Role.fromAuthority(authority.getAuthority()))
                .flatMap(Optional::stream)
                .collect(Collectors.toUnmodifiableSet());

        return new AuthenticatedUser(
                jwt.getSubject(),
                firstNonBlank(jwt.getClaimAsString(USERNAME_CLAIM), authentication.getName()),
                roles,
                jwt.getClaimAsString(TENANT_ID_CLAIM),
                TenantType.fromClaim(jwt.getClaimAsString(TENANT_TYPE_CLAIM)).orElse(null));
    }

    private String firstNonBlank(String firstValue, String fallbackValue) {
        if (firstValue != null && !firstValue.isBlank()) {
            return firstValue;
        }
        return fallbackValue;
    }
}
