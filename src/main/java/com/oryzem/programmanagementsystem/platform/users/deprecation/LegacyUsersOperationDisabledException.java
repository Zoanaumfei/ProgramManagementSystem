package com.oryzem.programmanagementsystem.platform.users.deprecation;

import org.springframework.http.HttpStatus;

public class LegacyUsersOperationDisabledException extends RuntimeException {

    private final HttpStatus status;
    private final String operation;
    private final LegacyUsersDeprecationStage stage;

    public LegacyUsersOperationDisabledException(
            HttpStatus status,
            String message,
            String operation,
            LegacyUsersDeprecationStage stage) {
        super(message);
        this.status = status;
        this.operation = operation;
        this.stage = stage;
    }

    public HttpStatus status() {
        return status;
    }

    public String operation() {
        return operation;
    }

    public LegacyUsersDeprecationStage stage() {
        return stage;
    }
}
