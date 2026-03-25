package com.oryzem.programmanagementsystem.platform.users.deprecation;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class LegacyUsersDeprecationHeadersFilter extends OncePerRequestFilter {

    private final LegacyUsersFeatureFlagService featureFlagService;

    public LegacyUsersDeprecationHeadersFilter(LegacyUsersFeatureFlagService featureFlagService) {
        this.featureFlagService = featureFlagService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path == null || !path.startsWith(LegacyUsersFeatureFlagService.LEGACY_USERS_PATH);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        featureFlagService.applyDeprecationHeaders(response);
        filterChain.doFilter(request, response);
    }
}
