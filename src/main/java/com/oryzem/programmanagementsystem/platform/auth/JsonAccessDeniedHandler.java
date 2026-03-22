package com.oryzem.programmanagementsystem.platform.auth;

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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import tools.jackson.databind.ObjectMapper;

public class JsonAccessDeniedHandler implements AccessDeniedHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonAccessDeniedHandler.class);

    private final ObjectMapper objectMapper;

    public JsonAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String principalName = authentication != null ? authentication.getName() : "anonymous";

        LOGGER.warn(
                "Access denied. method={}, path={}, principal={}, reason={}",
                request.getMethod(),
                request.getRequestURI(),
                principalName,
                accessDeniedException.getMessage());

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), errorBody(request, HttpStatus.FORBIDDEN, "Forbidden"));
    }

    private Map<String, Object> errorBody(HttpServletRequest request, HttpStatus status, String error) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", error);
        body.put("path", request.getRequestURI());
        return body;
    }
}
