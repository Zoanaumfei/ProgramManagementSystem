package com.oryzem.programmanagementsystem.platform.auth;

import com.oryzem.programmanagementsystem.platform.audit.RequestCorrelationContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

public class AuthenticationLoggingFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationLoggingFilter.class);
    private static final String ACCESS_CONTEXT_HEADER = "X-Access-Context";

    private final RequestCorrelationContext requestCorrelationContext;

    public AuthenticationLoggingFilter(RequestCorrelationContext requestCorrelationContext) {
        this.requestCorrelationContext = requestCorrelationContext;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        filterChain.doFilter(request, response);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String requestedAccessContext = request.getHeader(ACCESS_CONTEXT_HEADER);
        String correlationId = requestCorrelationContext.getOrCreate();
        if (authentication instanceof JwtAuthenticationToken jwtAuthentication && authentication.isAuthenticated()) {
            String authorities = authentication.getAuthorities().stream()
                    .map(Object::toString)
                    .sorted()
                    .collect(Collectors.joining(","));

            LOGGER.info(
                    "Authenticated request. method={}, path={}, subject={}, username={}, authorities={}, status={}, correlationId={}, accessContextPresent={}, accessContext={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    jwtAuthentication.getToken().getSubject(),
                    authentication.getName(),
                    authorities,
                    response.getStatus(),
                    correlationId,
                    requestedAccessContext != null && !requestedAccessContext.isBlank(),
                    sanitize(requestedAccessContext));
        } else {
            LOGGER.info(
                    "Anonymous request. method={}, path={}, status={}, correlationId={}, accessContextPresent={}, accessContext={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    correlationId,
                    requestedAccessContext != null && !requestedAccessContext.isBlank(),
                    sanitize(requestedAccessContext));
        }
    }

    private String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value.trim();
    }
}
