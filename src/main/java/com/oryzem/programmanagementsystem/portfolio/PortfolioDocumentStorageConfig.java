package com.oryzem.programmanagementsystem.portfolio;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Configuration
@EnableConfigurationProperties(PortfolioDocumentProperties.class)
class PortfolioDocumentStorageConfig {

    @Bean
    @ConditionalOnProperty(prefix = "app.portfolio.documents", name = "provider", havingValue = "s3")
    PortfolioDocumentStorageGateway s3PortfolioDocumentStorageGateway(PortfolioDocumentProperties properties) {
        if (!StringUtils.hasText(properties.bucketName())) {
            throw new IllegalStateException("S3 document storage requires app.portfolio.documents.bucket-name.");
        }

        Region region = Region.of(properties.region());
        S3Presigner presigner = S3Presigner.builder()
                .region(region)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
        S3Client s3Client = S3Client.builder()
                .region(region)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();

        return new S3PortfolioDocumentStorageGateway(properties, presigner, s3Client);
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.portfolio.documents", name = "provider", havingValue = "stub", matchIfMissing = true)
    PortfolioDocumentStorageGateway stubPortfolioDocumentStorageGateway(PortfolioDocumentProperties properties) {
        return new StubPortfolioDocumentStorageGateway(properties);
    }
}

@ConfigurationProperties(prefix = "app.portfolio.documents")
record PortfolioDocumentProperties(
        DocumentStorageProvider provider,
        String bucketName,
        String keyPrefix,
        long presignDurationMinutes,
        String region) {

    Duration presignDuration() {
        return Duration.ofMinutes(presignDurationMinutes > 0 ? presignDurationMinutes : 15);
    }
}

interface PortfolioDocumentStorageGateway {

    PreparedDocumentUpload prepareUpload(DeliverableDocumentEntity document);

    PreparedDocumentDownload prepareDownload(DeliverableDocumentEntity document);

    void assertObjectExists(DeliverableDocumentEntity document);
}

record PreparedDocumentUpload(
        String uploadUrl,
        Instant expiresAt,
        Map<String, String> requiredHeaders) {
}

record PreparedDocumentDownload(
        String downloadUrl,
        Instant expiresAt) {
}

final class StubPortfolioDocumentStorageGateway implements PortfolioDocumentStorageGateway {

    private final PortfolioDocumentProperties properties;

    StubPortfolioDocumentStorageGateway(PortfolioDocumentProperties properties) {
        this.properties = properties;
    }

    @Override
    public PreparedDocumentUpload prepareUpload(DeliverableDocumentEntity document) {
        Instant expiresAt = Instant.now().plus(properties.presignDuration());
        return new PreparedDocumentUpload(
                "https://stub-s3.local/%s/%s?operation=upload".formatted(document.getStorageBucket(), document.getStorageKey()),
                expiresAt,
                Map.of("Content-Type", document.getContentType()));
    }

    @Override
    public PreparedDocumentDownload prepareDownload(DeliverableDocumentEntity document) {
        Instant expiresAt = Instant.now().plus(properties.presignDuration());
        return new PreparedDocumentDownload(
                "https://stub-s3.local/%s/%s?operation=download".formatted(document.getStorageBucket(), document.getStorageKey()),
                expiresAt);
    }

    @Override
    public void assertObjectExists(DeliverableDocumentEntity document) {
        // Stub mode intentionally does not verify remote storage.
    }
}

final class S3PortfolioDocumentStorageGateway implements PortfolioDocumentStorageGateway, AutoCloseable {

    private final PortfolioDocumentProperties properties;
    private final S3Presigner presigner;
    private final S3Client s3Client;

    S3PortfolioDocumentStorageGateway(
            PortfolioDocumentProperties properties,
            S3Presigner presigner,
            S3Client s3Client) {
        this.properties = properties;
        this.presigner = presigner;
        this.s3Client = s3Client;
    }

    @Override
    public PreparedDocumentUpload prepareUpload(DeliverableDocumentEntity document) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(document.getStorageBucket())
                .key(document.getStorageKey())
                .contentType(document.getContentType())
                .build();
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(properties.presignDuration())
                .putObjectRequest(putObjectRequest)
                .build();
        PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(presignRequest);

        return new PreparedDocumentUpload(
                presignedRequest.url().toExternalForm(),
                Instant.now().plus(properties.presignDuration()),
                Map.of("Content-Type", document.getContentType()));
    }

    @Override
    public PreparedDocumentDownload prepareDownload(DeliverableDocumentEntity document) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(document.getStorageBucket())
                .key(document.getStorageKey())
                .responseContentType(document.getContentType())
                .build();
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(properties.presignDuration())
                .getObjectRequest(getObjectRequest)
                .build();
        PresignedGetObjectRequest presignedRequest = presigner.presignGetObject(presignRequest);

        return new PreparedDocumentDownload(
                presignedRequest.url().toExternalForm(),
                Instant.now().plus(properties.presignDuration()));
    }

    @Override
    public void assertObjectExists(DeliverableDocumentEntity document) {
        s3Client.headObject(HeadObjectRequest.builder()
                .bucket(document.getStorageBucket())
                .key(document.getStorageKey())
                .build());
    }

    @Override
    public void close() {
        presigner.close();
        s3Client.close();
    }
}
