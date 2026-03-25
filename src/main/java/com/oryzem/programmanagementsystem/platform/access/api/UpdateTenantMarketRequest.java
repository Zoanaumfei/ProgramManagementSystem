package com.oryzem.programmanagementsystem.platform.access.api;

import com.oryzem.programmanagementsystem.platform.access.MarketStatus;
import jakarta.validation.constraints.NotBlank;

public record UpdateTenantMarketRequest(
        @NotBlank String code,
        @NotBlank String name,
        MarketStatus status,
        String currencyCode,
        String languageCode,
        String timezone) {
}
