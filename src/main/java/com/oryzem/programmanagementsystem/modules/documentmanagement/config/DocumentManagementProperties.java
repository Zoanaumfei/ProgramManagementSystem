package com.oryzem.programmanagementsystem.modules.documentmanagement.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.document-management")
public class DocumentManagementProperties {

    private long maxFileSizeBytes = 25L * 1024L * 1024L;
    private int maxFilesPerContext = 20;
    private long uploadUrlTtlSeconds = 900;
    private long downloadUrlTtlSeconds = 180;
    private long pendingUploadExpirationMinutes = 30;
    private final Storage storage = new Storage();

    public long getMaxFileSizeBytes() {
        return maxFileSizeBytes;
    }

    public void setMaxFileSizeBytes(long maxFileSizeBytes) {
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    public int getMaxFilesPerContext() {
        return maxFilesPerContext;
    }

    public void setMaxFilesPerContext(int maxFilesPerContext) {
        this.maxFilesPerContext = maxFilesPerContext;
    }

    public long getUploadUrlTtlSeconds() {
        return uploadUrlTtlSeconds;
    }

    public void setUploadUrlTtlSeconds(long uploadUrlTtlSeconds) {
        this.uploadUrlTtlSeconds = uploadUrlTtlSeconds;
    }

    public long getDownloadUrlTtlSeconds() {
        return downloadUrlTtlSeconds;
    }

    public void setDownloadUrlTtlSeconds(long downloadUrlTtlSeconds) {
        this.downloadUrlTtlSeconds = downloadUrlTtlSeconds;
    }

    public long getPendingUploadExpirationMinutes() {
        return pendingUploadExpirationMinutes;
    }

    public void setPendingUploadExpirationMinutes(long pendingUploadExpirationMinutes) {
        this.pendingUploadExpirationMinutes = pendingUploadExpirationMinutes;
    }

    public Storage getStorage() {
        return storage;
    }

    public static class Storage {
        private Provider provider = Provider.S3;
        private final S3 s3 = new S3();

        public Provider getProvider() {
            return provider;
        }

        public void setProvider(Provider provider) {
            this.provider = provider;
        }

        public S3 getS3() {
            return s3;
        }
    }

    public enum Provider {
        S3
    }

    public static class S3 {
        private String bucket;
        private String region = "sa-east-1";
        private String kmsKeyId;
        private String keyPrefix = "tenant";

        public String getBucket() {
            return bucket;
        }

        public void setBucket(String bucket) {
            this.bucket = bucket;
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public String getKmsKeyId() {
            return kmsKeyId;
        }

        public void setKmsKeyId(String kmsKeyId) {
            this.kmsKeyId = kmsKeyId;
        }

        public String getKeyPrefix() {
            return keyPrefix;
        }

        public void setKeyPrefix(String keyPrefix) {
            this.keyPrefix = keyPrefix;
        }
    }
}
