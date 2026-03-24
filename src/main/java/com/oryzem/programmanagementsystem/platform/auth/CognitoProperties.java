package com.oryzem.programmanagementsystem.platform.auth;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.security.cognito")
public record CognitoProperties(
        @NotBlank String issuerUri,
        @NotBlank String jwkSetUri,
        @NotBlank String appClientId,
        String appClientSecret,
        List<String> allowedOrigins) {
}
