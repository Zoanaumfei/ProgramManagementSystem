package com.oryzem.programmanagementsystem.app.web;

import com.oryzem.programmanagementsystem.platform.audit.RequestCorrelationContext;
import com.oryzem.programmanagementsystem.platform.auth.AuthenticationFailedException;
import com.oryzem.programmanagementsystem.platform.shared.ConflictException;
import com.oryzem.programmanagementsystem.platform.shared.BusinessRuleException;
import com.oryzem.programmanagementsystem.platform.shared.FeatureTemporarilyUnavailableException;
import com.oryzem.programmanagementsystem.platform.shared.RateLimitExceededException;
import com.oryzem.programmanagementsystem.platform.shared.ResourceNotFoundException;
import com.oryzem.programmanagementsystem.platform.users.domain.UserNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.http.converter.HttpMessageNotReadableException;
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

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(
            ResourceNotFoundException exception,
            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(errorBody(request, HttpStatus.NOT_FOUND, "Not Found", exception.getMessage(), null));
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

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessRule(
            BusinessRuleException exception,
            HttpServletRequest request) {
        Map<String, Object> extras = new LinkedHashMap<>();
        extras.put("code", exception.code());
        if (!exception.details().isEmpty()) {
            extras.put("details", exception.details());
        }
        return ResponseEntity.badRequest()
                .body(errorBody(request, HttpStatus.BAD_REQUEST, "Bad Request", exception.getMessage(), extras));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException exception,
            HttpServletRequest request) {
        return ResponseEntity.badRequest()
                .body(errorBody(request, HttpStatus.BAD_REQUEST, "Bad Request", exception.getMessage(), null));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleMessageNotReadable(
            HttpMessageNotReadableException exception,
            HttpServletRequest request) {
        return ResponseEntity.badRequest()
                .body(errorBody(
                        request,
                        HttpStatus.BAD_REQUEST,
                        "Bad Request",
                        "Malformed JSON request body.",
                        null));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(
            DataIntegrityViolationException exception,
            HttpServletRequest request) {
        if (isOrganizationPurgeMembershipConstraint(request, exception)) {
            Map<String, Object> extras = new LinkedHashMap<>();
            extras.put("code", "ORGANIZATION_PURGE_BLOCKED_BY_MEMBERSHIPS");
            extras.put("hint", "Remova ou revise os vínculos de acesso relacionados a esta organização antes de tentar o purge novamente.");
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(errorBody(
                            request,
                            HttpStatus.CONFLICT,
                            "Conflict",
                            "Quase la: ainda existem vínculos de acesso ligados a esta organização, então não consegui concluir o purge.",
                            extras));
        }

        Map<String, Object> membershipReferenceError = membershipReferenceError(request, exception);
        if (membershipReferenceError != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(errorBody(
                            request,
                            HttpStatus.CONFLICT,
                            "Conflict",
                            String.valueOf(membershipReferenceError.remove("message")),
                            membershipReferenceError));
        }

        log.warn(
                "Generic data integrity violation for [{} {}] correlationId={} cause={}",
                request != null ? request.getMethod() : "unknown",
                request != null ? request.getRequestURI() : "unknown",
                requestCorrelationContext.getOrCreate(),
                mostSpecificMessage(exception));

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(errorBody(
                        request,
                        HttpStatus.CONFLICT,
                        "Conflict",
                        "Nao foi possivel concluir a operacao porque ainda existem dados relacionados a este registro.",
                        Map.of("code", "DATA_INTEGRITY_VIOLATION")));
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

    @ExceptionHandler(FeatureTemporarilyUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handleFeatureTemporarilyUnavailable(
            FeatureTemporarilyUnavailableException exception,
            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(errorBody(
                        request,
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Service Unavailable",
                        exception.getMessage(),
                        null));
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

    private boolean isOrganizationPurgeMembershipConstraint(
            HttpServletRequest request,
            DataIntegrityViolationException exception) {
        if (request == null || request.getRequestURI() == null || !request.getRequestURI().contains("/purge-subtree")) {
            return false;
        }
        String message = exception.getMessage();
        if (message == null) {
            return false;
        }
        return message.contains("fk_user_membership_organization")
                || message.contains("\"user_membership\"");
    }

    private Map<String, Object> membershipReferenceError(
            HttpServletRequest request,
            DataIntegrityViolationException exception) {
        if (request == null || request.getRequestURI() == null || !request.getRequestURI().contains("/memberships/")) {
            return null;
        }

        String message = mostSpecificMessage(exception);
        if (message == null || message.isBlank()) {
            return null;
        }

        if (message.contains("fk_user_membership_tenant")) {
            return integrityError(
                    "MEMBERSHIP_TENANT_REFERENCE_INVALID",
                    "Nao foi possivel salvar o membership porque o tenant informado nao existe ou nao esta mais disponivel.",
                    "Revise o tenant selecionado e tente novamente.");
        }
        if (message.contains("fk_user_membership_organization")) {
            return integrityError(
                    "MEMBERSHIP_ORGANIZATION_REFERENCE_INVALID",
                    "Nao foi possivel salvar o membership porque a organizacao informada nao existe ou nao esta mais disponivel.",
                    "Revise a organizacao selecionada e tente novamente.");
        }
        if (message.contains("fk_user_membership_market")) {
            return integrityError(
                    "MEMBERSHIP_MARKET_REFERENCE_INVALID",
                    "Nao foi possivel salvar o membership porque o mercado informado nao existe ou nao esta mais disponivel.",
                    "Revise o mercado selecionado e tente novamente.");
        }
        if (message.contains("fk_membership_role_role")) {
            return integrityError(
                    "MEMBERSHIP_ROLE_REFERENCE_INVALID",
                    "Nao foi possivel salvar o membership porque um ou mais perfis informados sao invalidos.",
                    "Revise os perfis selecionados e tente novamente.");
        }
        if (message.contains("uq_membership_role_membership_code")) {
            return integrityError(
                    "MEMBERSHIP_ROLE_DUPLICATE",
                    "Nao foi possivel salvar o membership porque o mesmo perfil foi associado mais de uma vez a este vinculo.",
                    "Tente novamente. Se o problema persistir, revise os perfis atuais do membership antes de salvar.");
        }

        return null;
    }

    private Map<String, Object> integrityError(String code, String message, String hint) {
        Map<String, Object> extras = new LinkedHashMap<>();
        extras.put("code", code);
        extras.put("hint", hint);
        extras.put("message", message);
        return extras;
    }

    private String mostSpecificMessage(DataIntegrityViolationException exception) {
        if (exception == null) {
            return null;
        }
        Throwable mostSpecificCause = exception.getMostSpecificCause();
        if (mostSpecificCause != null && mostSpecificCause.getMessage() != null && !mostSpecificCause.getMessage().isBlank()) {
            return mostSpecificCause.getMessage();
        }
        return exception.getMessage();
    }
}
