package com.oryzem.programmanagementsystem.platform.documents;

import java.time.Instant;
import java.util.Map;

public record PreparedDocumentUpload(
        String uploadUrl,
        Instant expiresAt,
        Map<String, String> requiredHeaders) {
}
