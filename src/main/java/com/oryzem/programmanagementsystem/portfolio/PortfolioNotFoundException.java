package com.oryzem.programmanagementsystem.portfolio;

public class PortfolioNotFoundException extends RuntimeException {

    public PortfolioNotFoundException(String resourceType, String resourceId) {
        super("%s '%s' was not found.".formatted(resourceType, resourceId));
    }
}
