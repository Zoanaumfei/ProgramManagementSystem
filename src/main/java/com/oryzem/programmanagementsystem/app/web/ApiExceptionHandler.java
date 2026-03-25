package com.oryzem.programmanagementsystem.app.web;

import com.oryzem.programmanagementsystem.modules.operations.OperationNotFoundException;
import com.oryzem.programmanagementsystem.platform.audit.RequestCorrelationContext;
import com.oryzem.programmanagementsystem.platform.auth.AuthenticationFailedException;
import com.oryzem.programmanagementsystem.platform.shared.ConflictException;
import com.oryzem.programmanagementsystem.platform.shared.RateLimitExceededException;
import com.oryzem.programmanagementsystem.platform.shared.ResourceNotFoundException;
import com.oryzem.programmanagementsystem.platform.users.deprecation.LegacyUsersOperationDisabledException;
import com.oryzem.programmanagementsystem.platform.users.domain.UserNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    private final RequestCorrelationContext requestCorrelationContext;

    public ApiExceptionHandler(RequestCorrelationContext requestCorrelationContext) {
        this.requestCorrelationContext = requestCorrelationContext;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(
            AccessDeniedException exception,
            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(errorBody(request, HttpStatus.FORBIDDEN, "Forbidden", exception.getMessage(), null));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUserNotFound(
            UserNotFoundException exception,
            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(errorBody(request, HttpStatus.NOT_FOUND, "Not Found", exception.getMessage(), null));
    }

    @ExceptionHandler(OperationNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleOperationNotFound(
            OperationNotFoundException exception,
            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(errorBody(request, HttpStatus.NOT_FOUND, "Not Found", exception.getMessage(), null));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(
            ResourceNotFoundException exception,
            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(errorBody(request, HttpStatus.NOT_FOUND, "Not Found", exception.getMessage(), null));
    }

    @ExceptionHandler(LegacyUsersOperationDisabledException.class)
    public ResponseEntity<Map<String, Object>> handleLegacyUsersOperationDisabled(
            LegacyUsersOperationDisabledException exception,
            HttpServletRequest request) {
        Map<String, Object> extras = new LinkedHashMap<>();
        extras.put("currentStage", exception.stage().name());
        extras.put("operation", exception.operation());
        extras.put("replacementPath", "/api/access/users/{userId}/memberships");
        return ResponseEntity.status(exception.status())
                .body(errorBody(
                        request,
                        exception.status(),
                        exception.status().getReasonPhrase(),
                        exception.getMessage(),
                        extras));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest()
                .body(errorBody(request, HttpStatus.BAD_REQUEST, "Bad Request", message, null));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException exception,
            HttpServletRequest request) {
        return ResponseEntity.badRequest()
                .body(errorBody(request, HttpStatus.BAD_REQUEST, "Bad Request", exception.getMessage(), null));
    }

    @ExceptionHandler(AuthenticationFailedException.class)
    public ResponseEntity<Map<String, Object>> handleAuthenticationFailed(
            AuthenticationFailedException exception,
            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(errorBody(request, HttpStatus.UNAUTHORIZED, "Unauthorized", exception.getMessage(), null));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(
            ConflictException exception,
            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(errorBody(request, HttpStatus.CONFLICT, "Conflict", exception.getMessage(), null));
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleRateLimitExceeded(
            RateLimitExceededException exception,
            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(errorBody(request, HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", exception.getMessage(), null));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResourceFound(
            NoResourceFoundException exception,
            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(errorBody(request, HttpStatus.NOT_FOUND, "Not Found", exception.getMessage(), null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpectedException(
            Exception exception,
            HttpServletRequest request) {
        String correlationId = requestCorrelationContext.getOrCreate();
        log.error(
                "Unhandled exception for [{} {}] correlationId={}",
                request.getMethod(),
                request.getRequestURI(),
                correlationId,
                exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorBody(
                        request,
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Internal Server Error",
                        "Unexpected server error.",
                        null));
    }

    private Map<String, Object> errorBody(
            HttpServletRequest request,
            HttpStatus status,
            String error,
            String message,
            Map<String, Object> extras) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", error);
        body.put("message", message);
        body.put("path", request.getRequestURI());
        String correlationId = requestCorrelationContext.get();
        if (correlationId != null && !correlationId.isBlank()) {
            body.put("correlationId", correlationId);
        }
        if (extras != null) {
            body.putAll(extras);
        }
        return body;
    }

    private String formatFieldError(FieldError fieldError) {
        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
    }
}


