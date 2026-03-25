package com.oryzem.programmanagementsystem.platform.authorization;

import com.oryzem.programmanagementsystem.platform.access.AccessContextService;
import com.oryzem.programmanagementsystem.platform.access.ResolvedMembershipContext;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public class AuthenticatedUserMapper {

    private static final String USERNAME_CLAIM = "cognito:username";
    private static final String ACCESS_TOKEN_USERNAME_CLAIM = "username";
    private static final String ACCESS_CONTEXT_HEADER = "X-Access-Context";

    private final AccessContextService accessContextService;
    private final ObjectProvider<HttpServletRequest> httpServletRequestProvider;

    public AuthenticatedUserMapper(
            AccessContextService accessContextService,
            ObjectProvider<HttpServletRequest> httpServletRequestProvider) {
        this.accessContextService = accessContextService;
        this.httpServletRequestProvider = httpServletRequestProvider;
    }

    public AuthenticatedUser from(Authentication authentication) {
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
            throw new IllegalArgumentException("Unsupported authentication type for authorization mapping.");
        }

        Jwt jwt = jwtAuthentication.getToken();
        String username = firstNonBlank(
                jwt.getClaimAsString(USERNAME_CLAIM),
                jwt.getClaimAsString(ACCESS_TOKEN_USERNAME_CLAIM),
                authentication.getName());
        String requestHeaderHint = Optional.ofNullable(httpServletRequestProvider.getIfAvailable())
                .map(request -> request.getHeader(ACCESS_CONTEXT_HEADER))
                .orElse(null);

        Optional<ResolvedMembershipContext> resolvedContext = accessContextService.resolveActiveContext(
                jwt.getSubject(),
                username,
                jwt.getClaimAsString("email"),
                requestHeaderHint);
        ResolvedMembershipContext membershipContext = resolvedContext.orElseThrow(() -> new IllegalStateException(
                "Authenticated user does not have a resolvable membership context. Hard cut membership-first requires a local membership."));
        return new AuthenticatedUser(
                jwt.getSubject(),
                username,
                membershipContext.roles(),
                membershipContext.permissions(),
                membershipContext.userId(),
                membershipContext.membershipId(),
                membershipContext.activeTenantId(),
                membershipContext.activeOrganizationId(),
                membershipContext.activeMarketId(),
                membershipContext.tenantType());
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
