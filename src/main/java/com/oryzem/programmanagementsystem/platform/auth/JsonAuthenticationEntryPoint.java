package com.oryzem.programmanagementsystem.platform.auth;

import com.oryzem.programmanagementsystem.platform.audit.RequestCorrelationContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import tools.jackson.databind.ObjectMapper;

public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonAuthenticationEntryPoint.class);
    private static final String ACCESS_CONTEXT_HEADER = "X-Access-Context";

    private final ObjectMapper objectMapper;
    private final RequestCorrelationContext requestCorrelationContext;

    public JsonAuthenticationEntryPoint(
            ObjectMapper objectMapper,
            RequestCorrelationContext requestCorrelationContext) {
        this.objectMapper = objectMapper;
        this.requestCorrelationContext = requestCorrelationContext;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        String requestedAccessContext = request.getHeader(ACCESS_CONTEXT_HEADER);
        String correlationId = requestCorrelationContext.getOrCreate();

        LOGGER.warn(
                "Authentication failed. method={}, path={}, reason={}, correlationId={}, accessContextPresent={}, accessContext={}",
                request.getMethod(),
                request.getRequestURI(),
                authException.getMessage(),
                correlationId,
                requestedAccessContext != null && !requestedAccessContext.isBlank(),
                sanitize(requestedAccessContext));

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), errorBody(request, HttpStatus.UNAUTHORIZED, "Unauthorized"));
    }

    private Map<String, Object> errorBody(HttpServletRequest request, HttpStatus status, String error) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", error);
        body.put("path", request.getRequestURI());
        body.put("correlationId", requestCorrelationContext.getOrCreate());
        return body;
    }

    private String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value.trim();
    }
}
