package com.oryzem.programmanagementsystem.platform.documents;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.portfolio.documents")
public record PortfolioDocumentProperties(
        DocumentStorageProvider provider,
        String bucketName,
        String keyPrefix,
        long presignDurationMinutes,
        String region) {

    public Duration presignDuration() {
        return Duration.ofMinutes(presignDurationMinutes > 0 ? presignDurationMinutes : 15);
    }
}
