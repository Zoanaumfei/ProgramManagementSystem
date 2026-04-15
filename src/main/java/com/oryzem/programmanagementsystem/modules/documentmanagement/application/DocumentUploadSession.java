package com.oryzem.programmanagementsystem.modules.documentmanagement.application;

import java.time.Instant;
import java.util.Map;

public record DocumentUploadSession(
        String documentId,
        String url,
        Map<String, String> fields,
        Instant expiresAt) {
}
