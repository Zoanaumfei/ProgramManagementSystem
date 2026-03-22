package com.oryzem.programmanagementsystem.platform.documents;

public record DocumentStorageObject(
        String storageBucket,
        String storageKey,
        String contentType) {
}
