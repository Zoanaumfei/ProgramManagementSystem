package com.oryzem.programmanagementsystem.platform.shared;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resourceType, String resourceId) {
        super(resourceType + " '" + resourceId + "' was not found.");
    }
}
