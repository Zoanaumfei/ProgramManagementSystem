package com.oryzem.programmanagementsystem.modules.operations;

public class OperationNotFoundException extends RuntimeException {

    public OperationNotFoundException(String operationId) {
        super("Operation not found: " + operationId);
    }
}
