package com.oryzem.programmanagementsystem.platform.audit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RequestCorrelationFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Correlation-Id";

    private final RequestCorrelationContext requestCorrelationContext;

    public RequestCorrelationFilter(RequestCorrelationContext requestCorrelationContext) {
        this.requestCorrelationContext = requestCorrelationContext;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String correlationId = resolveCorrelationId(request);
        requestCorrelationContext.set(correlationId);
        response.setHeader(HEADER_NAME, correlationId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            requestCorrelationContext.clear();
        }
    }

    private String resolveCorrelationId(HttpServletRequest request) {
        String headerValue = request.getHeader(HEADER_NAME);
        if (headerValue != null && !headerValue.isBlank()) {
            return headerValue.trim();
        }
        return UUID.randomUUID().toString();
    }
}
