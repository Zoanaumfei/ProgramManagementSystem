package com.oryzem.programmanagementsystem.platform.shared;

public class FeatureTemporarilyUnavailableException extends RuntimeException {

    public FeatureTemporarilyUnavailableException(String message) {
        super(message);
    }
}
