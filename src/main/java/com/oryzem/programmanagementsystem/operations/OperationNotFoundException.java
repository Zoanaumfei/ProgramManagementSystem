package com.oryzem.programmanagementsystem.operations;

public class OperationNotFoundException extends RuntimeException {

    public OperationNotFoundException(String operationId) {
        super("Operation not found: " + operationId);
    }
}
