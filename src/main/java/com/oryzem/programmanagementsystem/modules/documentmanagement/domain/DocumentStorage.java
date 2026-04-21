package com.oryzem.programmanagementsystem.modules.documentmanagement.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

public interface DocumentStorage {

    UploadInstruction createUploadInstruction(
            String storageKey,
            String contentType,
            long sizeBytes,
            String checksumSha256,
            String documentId,
            Duration expiresIn);

    DownloadInstruction createDownloadInstruction(
            String storageKey,
            Duration expiresIn,
            String downloadFilename);

    StoredObjectInfo headObject(String storageKey);

    byte[] readSignatureBytes(String storageKey, int maxBytes);

    Set<String> listStorageKeys(String prefix);

    void deleteObject(String storageKey);

    record UploadInstruction(
            String url,
            Map<String, String> fields,
            Instant expiresAt) {
    }

    record DownloadInstruction(
            String url,
            Instant expiresAt) {
    }

    record StoredObjectInfo(
            boolean exists,
            long sizeBytes,
            String contentType,
            Map<String, String> metadata,
            Instant lastModifiedAt) {

        public static StoredObjectInfo missing() {
            return new StoredObjectInfo(false, 0L, null, Map.of(), null);
        }
    }
}
