package com.oryzem.programmanagementsystem.platform.access;

import com.oryzem.programmanagementsystem.platform.audit.RequestCorrelationContext;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUserMapper;
import com.oryzem.programmanagementsystem.platform.shared.FeatureTemporarilyUnavailableException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

@Component
public class TenantRateLimitingFilter extends OncePerRequestFilter {

    private final AuthenticatedUserMapper authenticatedUserMapper;
    private final TenantGovernanceService tenantGovernanceService;
    private final TenantRateLimitCounterStore tenantRateLimitCounterStore;
    private final RequestCorrelationContext requestCorrelationContext;
    private final ObjectMapper objectMapper;

    public TenantRateLimitingFilter(
            AuthenticatedUserMapper authenticatedUserMapper,
            TenantGovernanceService tenantGovernanceService,
            TenantRateLimitCounterStore tenantRateLimitCounterStore,
            RequestCorrelationContext requestCorrelationContext,
            ObjectMapper objectMapper) {
        this.authenticatedUserMapper = authenticatedUserMapper;
        this.tenantGovernanceService = tenantGovernanceService;
        this.tenantRateLimitCounterStore = tenantRateLimitCounterStore;
        this.requestCorrelationContext = requestCorrelationContext;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path == null
                || (!path.startsWith("/api/access") && !"/api/auth/me".equals(path));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        AuthenticatedUser actor;
        try {
            actor = authenticatedUserMapper.from(authentication);
        } catch (RuntimeException exception) {
            filterChain.doFilter(request, response);
            return;
        }

        if (actor.activeTenantId() == null || actor.activeTenantId().isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        TenantGovernanceService.RateLimitPolicy policy = tenantGovernanceService.rateLimitPolicy(actor.activeTenantId());
        if (allow(actor.activeTenantId(), policy)) {
            filterChain.doFilter(request, response);
            return;
        }

        writeTooManyRequests(response, request);
    }

    private boolean allow(String tenantId, TenantGovernanceService.RateLimitPolicy policy) {
        try {
            long counter = tenantRateLimitCounterStore.increment(tenantId, policy.window());
            return counter <= policy.maxRequests();
        } catch (RuntimeException exception) {
            throw new FeatureTemporarilyUnavailableException("Tenant rate limit store is unavailable.");
        }
    }

    void clearCounters() {
        tenantRateLimitCounterStore.clear();
    }

    private void writeTooManyRequests(HttpServletResponse response, HttpServletRequest request) throws IOException {
        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String correlationId = requestCorrelationContext.getOrCreate();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", 429);
        body.put("error", "Too Many Requests");
        body.put("message", "Tenant rate limit exceeded.");
        body.put("path", request.getRequestURI());
        body.put("correlationId", correlationId);
        objectMapper.writeValue(response.getWriter(), body);
    }
}
