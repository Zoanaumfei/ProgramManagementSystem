package com.oryzem.programmanagementsystem.platform.access;

import com.oryzem.programmanagementsystem.platform.tenant.OrganizationLookup;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ActiveAccessContextDescriptorService {

    private final SpringDataTenantJpaRepository tenantRepository;
    private final SpringDataTenantMarketJpaRepository tenantMarketRepository;
    private final OrganizationLookup organizationLookup;

    public ActiveAccessContextDescriptorService(
            SpringDataTenantJpaRepository tenantRepository,
            SpringDataTenantMarketJpaRepository tenantMarketRepository,
            OrganizationLookup organizationLookup) {
        this.tenantRepository = tenantRepository;
        this.tenantMarketRepository = tenantMarketRepository;
        this.organizationLookup = organizationLookup;
    }

    public ActiveAccessContextLabels describe(String tenantId, String organizationId, String marketId) {
        String tenantName = hasText(tenantId)
                ? tenantRepository.findById(tenantId).map(TenantEntity::getName).orElse(null)
                : null;
        String organizationName = hasText(organizationId)
                ? organizationLookup.findById(organizationId).map(OrganizationLookup.OrganizationView::name).orElse(null)
                : null;
        String marketName = hasText(marketId)
                ? tenantMarketRepository.findById(marketId).map(TenantMarketEntity::getName).orElse(null)
                : null;
        return new ActiveAccessContextLabels(tenantName, organizationName, marketName);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public record ActiveAccessContextLabels(
            String tenantName,
            String organizationName,
            String marketName) {
    }
}
