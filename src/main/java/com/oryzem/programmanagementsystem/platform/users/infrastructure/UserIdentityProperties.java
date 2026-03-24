package com.oryzem.programmanagementsystem.platform.users.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.identity")
public record UserIdentityProperties(
        String provider,
        String userPoolId,
        String region) {
}
