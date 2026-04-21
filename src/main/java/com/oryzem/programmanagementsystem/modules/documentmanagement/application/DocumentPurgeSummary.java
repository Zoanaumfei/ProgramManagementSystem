package com.oryzem.programmanagementsystem.modules.documentmanagement.application;

public record DocumentPurgeSummary(
        long documentCount,
        long storageObjectCount) {
}
