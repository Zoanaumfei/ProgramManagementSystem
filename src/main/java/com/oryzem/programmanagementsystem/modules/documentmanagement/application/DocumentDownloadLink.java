package com.oryzem.programmanagementsystem.modules.documentmanagement.application;

import java.time.Instant;

public record DocumentDownloadLink(
        String url,
        Instant expiresAt) {
}
