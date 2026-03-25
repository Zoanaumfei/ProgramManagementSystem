package com.oryzem.programmanagementsystem.platform.access.api;

import com.oryzem.programmanagementsystem.platform.access.MarketStatus;
import java.time.Instant;

public record TenantMarketResponse(
        String id,
        String tenantId,
        String code,
        String name,
        MarketStatus status,
        String currencyCode,
        String languageCode,
        String timezone,
        Instant createdAt,
        Instant updatedAt) {
}
