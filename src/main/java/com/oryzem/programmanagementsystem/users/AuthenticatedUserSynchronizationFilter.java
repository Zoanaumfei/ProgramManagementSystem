package com.oryzem.programmanagementsystem.users;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class AuthenticatedUserSynchronizationFilter extends OncePerRequestFilter {

    private static final String COGNITO_USERNAME_CLAIM = "cognito:username";
    private static final String ACCESS_TOKEN_USERNAME_CLAIM = "username";

    private final UserManagementService userManagementService;

    public AuthenticatedUserSynchronizationFilter(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuthentication && authentication.isAuthenticated()) {
            userManagementService.synchronizeAuthenticatedUser(
                    jwtAuthentication.getToken().getSubject(),
                    firstNonBlank(
                            jwtAuthentication.getToken().getClaimAsString(COGNITO_USERNAME_CLAIM),
                            jwtAuthentication.getToken().getClaimAsString(ACCESS_TOKEN_USERNAME_CLAIM)),
                    jwtAuthentication.getToken().getClaimAsString("email"));
        }

        filterChain.doFilter(request, response);
    }

    private String firstNonBlank(String firstValue, String fallbackValue) {
        if (firstValue != null && !firstValue.isBlank()) {
            return firstValue;
        }
        return fallbackValue;
    }
}
