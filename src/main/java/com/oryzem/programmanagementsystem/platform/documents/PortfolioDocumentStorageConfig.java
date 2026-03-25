package com.oryzem.programmanagementsystem.platform.documents;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

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
        DefaultCredentialsProvider credentialsProvider = DefaultCredentialsProvider.builder().build();
        S3Presigner presigner = S3Presigner.builder()
                .region(region)
                .credentialsProvider(credentialsProvider)
                .build();
        S3Client s3Client = S3Client.builder()
                .region(region)
                .credentialsProvider(credentialsProvider)
                .build();

        return new S3PortfolioDocumentStorageGateway(properties, presigner, s3Client);
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.portfolio.documents", name = "provider", havingValue = "stub", matchIfMissing = true)
    PortfolioDocumentStorageGateway stubPortfolioDocumentStorageGateway(PortfolioDocumentProperties properties) {
        return new StubPortfolioDocumentStorageGateway(properties);
    }

    private static final class StubPortfolioDocumentStorageGateway implements PortfolioDocumentStorageGateway {

        private final PortfolioDocumentProperties properties;

        private StubPortfolioDocumentStorageGateway(PortfolioDocumentProperties properties) {
            this.properties = properties;
        }

        @Override
        public PreparedDocumentUpload prepareUpload(DocumentStorageObject document) {
            Instant expiresAt = Instant.now().plus(properties.presignDuration());
            return new PreparedDocumentUpload(
                    "https://stub-s3.local/%s/%s?operation=upload".formatted(document.storageBucket(), document.storageKey()),
                    expiresAt,
                    Map.of("Content-Type", document.contentType()));
        }

        @Override
        public PreparedDocumentDownload prepareDownload(DocumentStorageObject document) {
            Instant expiresAt = Instant.now().plus(properties.presignDuration());
            return new PreparedDocumentDownload(
                    "https://stub-s3.local/%s/%s?operation=download".formatted(document.storageBucket(), document.storageKey()),
                    expiresAt);
        }

        @Override
        public void assertObjectExists(DocumentStorageObject document) {
        }

        @Override
        public void deleteObject(DocumentStorageObject document) {
        }
    }

    private static final class S3PortfolioDocumentStorageGateway implements PortfolioDocumentStorageGateway, AutoCloseable {

        private final PortfolioDocumentProperties properties;
        private final S3Presigner presigner;
        private final S3Client s3Client;

        private S3PortfolioDocumentStorageGateway(
                PortfolioDocumentProperties properties,
                S3Presigner presigner,
                S3Client s3Client) {
            this.properties = properties;
            this.presigner = presigner;
            this.s3Client = s3Client;
        }

        @Override
        public PreparedDocumentUpload prepareUpload(DocumentStorageObject document) {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(document.storageBucket())
                    .key(document.storageKey())
                    .contentType(document.contentType())
                    .build();
            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(properties.presignDuration())
                    .putObjectRequest(putObjectRequest)
                    .build();
            PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(presignRequest);

            return new PreparedDocumentUpload(
                    presignedRequest.url().toExternalForm(),
                    Instant.now().plus(properties.presignDuration()),
                    Map.of("Content-Type", document.contentType()));
        }

        @Override
        public PreparedDocumentDownload prepareDownload(DocumentStorageObject document) {
            GetObjectRequest.Builder getObjectRequestBuilder = GetObjectRequest.builder()
                    .bucket(document.storageBucket())
                    .key(document.storageKey())
                    .responseContentType(document.contentType());
            if (StringUtils.hasText(document.fileName())) {
                getObjectRequestBuilder.responseContentDisposition(contentDisposition(document.fileName()));
            }
            GetObjectRequest getObjectRequest = getObjectRequestBuilder.build();
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
        public void assertObjectExists(DocumentStorageObject document) {
            try {
                s3Client.headObject(HeadObjectRequest.builder()
                        .bucket(document.storageBucket())
                        .key(document.storageKey())
                        .build());
            } catch (S3Exception exception) {
                if (exception.statusCode() == 404) {
                    throw new IllegalArgumentException("Uploaded document object was not found in storage.");
                }
                if (exception.statusCode() == 403) {
                    throw new IllegalStateException("Document storage access was denied while validating the uploaded object.");
                }
                throw exception;
            }
        }

        @Override
        public void deleteObject(DocumentStorageObject document) {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(document.storageBucket())
                    .key(document.storageKey())
                    .build());
        }

        @Override
        public void close() {
            presigner.close();
            s3Client.close();
        }

        private String contentDisposition(String fileName) {
            return "attachment; filename=\"%s\"".formatted(fileName.replace("\"", ""));
        }
    }
}
