package com.oryzem.programmanagementsystem.modules.documentmanagement.storage;

import com.oryzem.programmanagementsystem.modules.documentmanagement.config.DocumentManagementProperties;
import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentStorage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

public class S3DocumentStorage implements DocumentStorage {

    private static final DateTimeFormatter DATE_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter AMZ_DATE = new DateTimeFormatterBuilder()
            .appendValue(ChronoField.YEAR, 4)
            .appendValue(ChronoField.MONTH_OF_YEAR, 2)
            .appendValue(ChronoField.DAY_OF_MONTH, 2)
            .appendLiteral('T')
            .appendValue(ChronoField.HOUR_OF_DAY, 2)
            .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
            .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
            .appendLiteral('Z')
            .toFormatter(Locale.ROOT)
            .withZone(ZoneOffset.UTC);

    private final DocumentManagementProperties properties;
    private final AwsCredentialsProvider credentialsProvider;
    private final S3Client s3Client;
    private final S3Presigner presigner;
    private final Region region;
    private final Clock clock;

    public S3DocumentStorage(DocumentManagementProperties properties) {
        this(
                properties,
                DefaultCredentialsProvider.create(),
                Region.of(properties.getStorage().getS3().getRegion()),
                Clock.systemUTC());
    }

    S3DocumentStorage(
            DocumentManagementProperties properties,
            AwsCredentialsProvider credentialsProvider,
            Region region,
            Clock clock) {
        this.properties = properties;
        this.credentialsProvider = credentialsProvider;
        this.region = region;
        this.clock = clock;
        this.s3Client = S3Client.builder()
                .region(region)
                .credentialsProvider(credentialsProvider)
                .build();
        this.presigner = S3Presigner.builder()
                .region(region)
                .credentialsProvider(credentialsProvider)
                .build();
    }

    @Override
    public UploadInstruction createUploadInstruction(
            String storageKey,
            String contentType,
            long sizeBytes,
            String checksumSha256,
            String documentId,
            Duration expiresIn) {
        Instant now = clock.instant();
        Instant expiresAt = now.plus(expiresIn);
        AwsCredentials credentials = credentialsProvider.resolveCredentials();
        String dateStamp = DATE_STAMP.format(now);
        String amzDate = AMZ_DATE.format(now);
        String credentialScope = dateStamp + "/" + region.id() + "/s3/aws4_request";
        String credential = credentials.accessKeyId() + "/" + credentialScope;

        StringBuilder policy = new StringBuilder();
        policy.append("{\"expiration\":\"")
                .append(DateTimeFormatter.ISO_INSTANT.format(expiresAt))
                .append("\",\"conditions\":[")
                .append("{\"bucket\":\"").append(escape(bucket())).append("\"},")
                .append("[\"eq\",\"$key\",\"").append(escape(storageKey)).append("\"],")
                .append("[\"eq\",\"$Content-Type\",\"").append(escape(contentType)).append("\"],")
                .append("[\"content-length-range\",").append(sizeBytes).append(",").append(sizeBytes).append("],")
                .append("{\"x-amz-algorithm\":\"AWS4-HMAC-SHA256\"},")
                .append("{\"x-amz-credential\":\"").append(escape(credential)).append("\"},")
                .append("{\"x-amz-date\":\"").append(amzDate).append("\"},")
                .append("{\"x-amz-server-side-encryption\":\"aws:kms\"},")
                .append("{\"x-amz-meta-checksum-sha256\":\"").append(escape(checksumSha256)).append("\"},")
                .append("{\"x-amz-meta-document-id\":\"").append(escape(documentId)).append("\"}");
        if (hasText(properties.getStorage().getS3().getKmsKeyId())) {
            policy.append(",{\"x-amz-server-side-encryption-aws-kms-key-id\":\"")
                    .append(escape(properties.getStorage().getS3().getKmsKeyId().trim()))
                    .append("\"}");
        }
        if (credentials instanceof AwsSessionCredentials sessionCredentials) {
            policy.append(",{\"x-amz-security-token\":\"")
                    .append(escape(sessionCredentials.sessionToken()))
                    .append("\"}");
        }
        policy.append("]}");

        String policyBase64 = Base64.getEncoder().encodeToString(policy.toString().getBytes(StandardCharsets.UTF_8));
        String signature = HexFormat.of().formatHex(sign(signingKey(credentials.secretAccessKey(), dateStamp), policyBase64));

        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("key", storageKey);
        fields.put("Content-Type", contentType);
        fields.put("policy", policyBase64);
        fields.put("x-amz-algorithm", "AWS4-HMAC-SHA256");
        fields.put("x-amz-credential", credential);
        fields.put("x-amz-date", amzDate);
        fields.put("x-amz-signature", signature);
        fields.put("x-amz-server-side-encryption", "aws:kms");
        fields.put("x-amz-meta-checksum-sha256", checksumSha256);
        fields.put("x-amz-meta-document-id", documentId);
        if (hasText(properties.getStorage().getS3().getKmsKeyId())) {
            fields.put("x-amz-server-side-encryption-aws-kms-key-id", properties.getStorage().getS3().getKmsKeyId().trim());
        }
        if (credentials instanceof AwsSessionCredentials sessionCredentials) {
            fields.put("x-amz-security-token", sessionCredentials.sessionToken());
        }

        return new UploadInstruction(postUrl(), Map.copyOf(fields), expiresAt);
    }

    @Override
    public DownloadInstruction createDownloadInstruction(String storageKey, Duration expiresIn, String downloadFilename) {
        Instant expiresAt = clock.instant().plus(expiresIn);
        PresignedGetObjectRequest request = presigner.presignGetObject(
                GetObjectPresignRequest.builder()
                        .signatureDuration(expiresIn)
                        .getObjectRequest(builder -> builder
                                .bucket(bucket())
                                .key(storageKey)
                                .responseContentDisposition("attachment; filename=\"" + sanitizeHeaderFilename(downloadFilename) + "\""))
                        .build());
        return new DownloadInstruction(request.url().toString(), expiresAt);
    }

    @Override
    public StoredObjectInfo headObject(String storageKey) {
        try {
            HeadObjectResponse response = s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucket())
                    .key(storageKey)
                    .build());
            return new StoredObjectInfo(
                    true,
                    response.contentLength(),
                    response.contentType(),
                    normalizeMetadata(response.metadata()),
                    response.lastModified());
        } catch (NoSuchKeyException exception) {
            return StoredObjectInfo.missing();
        } catch (S3Exception exception) {
            if (exception.statusCode() == 404) {
                return StoredObjectInfo.missing();
            }
            throw exception;
        }
    }

    @Override
    public byte[] readSignatureBytes(String storageKey, int maxBytes) {
        try (var stream = s3Client.getObject(GetObjectRequest.builder()
                .bucket(bucket())
                .key(storageKey)
                .range("bytes=0-" + Math.max(0, maxBytes - 1))
                .build())) {
            return stream.readAllBytes();
        } catch (NoSuchKeyException exception) {
            return new byte[0];
        } catch (S3Exception exception) {
            if (exception.statusCode() == 404) {
                return new byte[0];
            }
            throw exception;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read document signature bytes from S3.", exception);
        }
    }

    @Override
    public Set<String> listStorageKeys(String prefix) {
        return s3Client.listObjectsV2Paginator(builder -> builder
                        .bucket(bucket())
                        .prefix(prefix))
                .stream()
                .flatMap(page -> page.contents().stream())
                .map(item -> item.key())
                .collect(Collectors.toSet());
    }

    @Override
    public void deleteObject(String storageKey) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket())
                .key(storageKey)
                .build());
    }

    private byte[] signingKey(String secretKey, String dateStamp) {
        byte[] kSecret = ("AWS4" + secretKey).getBytes(StandardCharsets.UTF_8);
        byte[] kDate = sign(kSecret, dateStamp);
        byte[] kRegion = sign(kDate, region.id());
        byte[] kService = sign(kRegion, "s3");
        return sign(kService, "aws4_request");
    }

    private byte[] sign(byte[] key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to sign S3 POST policy.", exception);
        }
    }

    private Map<String, String> normalizeMetadata(Map<String, String> metadata) {
        return metadata.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> entry.getKey().toLowerCase(Locale.ROOT),
                        Map.Entry::getValue,
                        (left, right) -> right,
                        TreeMap::new));
    }

    private String sanitizeHeaderFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "document";
        }
        return filename.replace("\"", "");
    }

    private String postUrl() {
        return "https://" + bucket() + ".s3." + region.id() + ".amazonaws.com/";
    }

    private String bucket() {
        return properties.getStorage().getS3().getBucket().trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
