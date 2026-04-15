package com.oryzem.programmanagementsystem.modules.documentmanagement.support;

import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentStorage;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InMemoryDocumentStorageStub implements DocumentStorage {

    private final Map<String, StoredFile> objects = new ConcurrentHashMap<>();

    @Override
    public UploadInstruction createUploadInstruction(
            String storageKey,
            String contentType,
            long sizeBytes,
            String checksumSha256,
            String documentId,
            Duration expiresIn) {
        Instant expiresAt = Instant.now().plus(expiresIn);
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("key", storageKey);
        fields.put("Content-Type", contentType);
        fields.put("x-amz-meta-checksum-sha256", checksumSha256);
        fields.put("x-amz-meta-document-id", documentId);
        return new UploadInstruction("https://upload.test/" + storageKey, Map.copyOf(fields), expiresAt);
    }

    @Override
    public DownloadInstruction createDownloadInstruction(String storageKey, Duration expiresIn, String downloadFilename) {
        return new DownloadInstruction("https://download.test/" + storageKey, Instant.now().plus(expiresIn));
    }

    @Override
    public StoredObjectInfo headObject(String storageKey) {
        StoredFile file = objects.get(storageKey);
        if (file == null) {
            return StoredObjectInfo.missing();
        }
        return new StoredObjectInfo(true, file.content.length, file.contentType, Map.copyOf(file.metadata), file.createdAt);
    }

    @Override
    public byte[] readSignatureBytes(String storageKey, int maxBytes) {
        StoredFile file = objects.get(storageKey);
        if (file == null) {
            return new byte[0];
        }
        return Arrays.copyOf(file.content, Math.min(file.content.length, maxBytes));
    }

    @Override
    public Set<String> listStorageKeys(String prefix) {
        return objects.keySet().stream()
                .filter(key -> key.startsWith(prefix))
                .collect(Collectors.toSet());
    }

    public void putObject(String storageKey, String contentType, byte[] content, Map<String, String> metadata) {
        objects.put(storageKey, new StoredFile(contentType, content.clone(), new HashMap<>(metadata), Instant.now()));
    }

    public void clear() {
        objects.clear();
    }

    private record StoredFile(
            String contentType,
            byte[] content,
            Map<String, String> metadata,
            Instant createdAt) {
    }
}
