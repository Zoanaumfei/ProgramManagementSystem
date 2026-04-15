package com.oryzem.programmanagementsystem.modules.documentmanagement.storage;

import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentStorage;
import com.oryzem.programmanagementsystem.platform.shared.FeatureTemporarilyUnavailableException;
import java.time.Duration;
import java.util.Set;

public class NoopDocumentStorage implements DocumentStorage {

    @Override
    public UploadInstruction createUploadInstruction(
            String storageKey,
            String contentType,
            long sizeBytes,
            String checksumSha256,
            String documentId,
            Duration expiresIn) {
        throw unavailable();
    }

    @Override
    public DownloadInstruction createDownloadInstruction(String storageKey, Duration expiresIn, String downloadFilename) {
        throw unavailable();
    }

    @Override
    public StoredObjectInfo headObject(String storageKey) {
        return StoredObjectInfo.missing();
    }

    @Override
    public byte[] readSignatureBytes(String storageKey, int maxBytes) {
        return new byte[0];
    }

    @Override
    public Set<String> listStorageKeys(String prefix) {
        return Set.of();
    }

    private FeatureTemporarilyUnavailableException unavailable() {
        return new FeatureTemporarilyUnavailableException(
                "Document storage is not configured. Defina app.document-management.storage.s3.bucket para habilitar o modulo documental.");
    }
}
