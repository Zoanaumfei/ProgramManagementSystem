package com.oryzem.programmanagementsystem.platform.documents;

import java.time.Instant;

public record PreparedDocumentDownload(
        String downloadUrl,
        Instant expiresAt) {
}
