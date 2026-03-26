package com.oryzem.programmanagementsystem.platform.access;

import com.oryzem.programmanagementsystem.platform.audit.RequestCorrelationContext;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUserMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

@Component
public class TenantRateLimitingFilter extends OncePerRequestFilter {

    private final ConcurrentHashMap<String, FixedWindowCounter> counters = new ConcurrentHashMap<>();
    private final AuthenticatedUserMapper authenticatedUserMapper;
    private final TenantGovernanceService tenantGovernanceService;
    private final RequestCorrelationContext requestCorrelationContext;
    private final ObjectMapper objectMapper;

    public TenantRateLimitingFilter(
            AuthenticatedUserMapper authenticatedUserMapper,
            TenantGovernanceService tenantGovernanceService,
            RequestCorrelationContext requestCorrelationContext,
            ObjectMapper objectMapper) {
        this.authenticatedUserMapper = authenticatedUserMapper;
        this.tenantGovernanceService = tenantGovernanceService;
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
        long windowMillis = policy.window().toMillis();
        long now = System.currentTimeMillis();
        FixedWindowCounter counter = counters.compute(tenantId, (key, current) -> {
            if (current == null || now >= current.windowEndsAtEpochMillis) {
                return new FixedWindowCounter(now + windowMillis);
            }
            return current;
        });
        return counter.requests.incrementAndGet() <= policy.maxRequests();
    }

    void clearCounters() {
        counters.clear();
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

    private static final class FixedWindowCounter {
        private final long windowEndsAtEpochMillis;
        private final AtomicInteger requests = new AtomicInteger();

        private FixedWindowCounter(long windowEndsAtEpochMillis) {
            this.windowEndsAtEpochMillis = windowEndsAtEpochMillis;
        }
    }
}
