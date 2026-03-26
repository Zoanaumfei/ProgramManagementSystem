package com.oryzem.programmanagementsystem.platform.access;

import com.oryzem.programmanagementsystem.platform.shared.ConflictException;
import com.oryzem.programmanagementsystem.platform.tenant.OrganizationRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TenantGovernanceService {

    private final SpringDataTenantJpaRepository tenantRepository;
    private final SpringDataTenantMarketJpaRepository tenantMarketRepository;
    private final SpringDataUserMembershipJpaRepository membershipRepository;
    private final OrganizationRepository organizationRepository;
    private final TenantGovernanceProperties properties;

    public TenantGovernanceService(
            SpringDataTenantJpaRepository tenantRepository,
            SpringDataTenantMarketJpaRepository tenantMarketRepository,
            SpringDataUserMembershipJpaRepository membershipRepository,
            OrganizationRepository organizationRepository,
            TenantGovernanceProperties properties) {
        this.tenantRepository = tenantRepository;
        this.tenantMarketRepository = tenantMarketRepository;
        this.membershipRepository = membershipRepository;
        this.organizationRepository = organizationRepository;
        this.properties = properties;
    }

    public TenantServiceTier resolveTier(String tenantId) {
        return tenantRepository.findById(tenantId)
                .map(TenantEntity::getServiceTier)
                .orElse(TenantServiceTier.STANDARD);
    }

    public RateLimitPolicy rateLimitPolicy(String tenantId) {
        TenantServiceTier tier = resolveTier(tenantId);
        int maxRequests = switch (tier) {
            case INTERNAL -> properties.getRateLimit().getInternalMaxRequests();
            case ENTERPRISE -> properties.getRateLimit().getEnterpriseMaxRequests();
            case STANDARD -> properties.getRateLimit().getStandardMaxRequests();
        };
        return new RateLimitPolicy(Duration.ofSeconds(properties.getRateLimit().getWindowSeconds()), maxRequests);
    }

    public Instant retentionDeadlineFrom(Instant reference) {
        return reference.plus(Duration.ofDays(properties.getOffboarding().getRetentionDays()));
    }

    public void assertOrganizationQuotaAvailable(String tenantId) {
        TenantGovernanceProperties.TierLimit limit = limitFor(resolveTier(tenantId));
        long currentCount = organizationRepository.findAllByTenantIdOrderByNameAsc(tenantId).size();
        if (currentCount >= limit.getMaxOrganizations()) {
            throw new ConflictException("Organization quota reached for tenant tier.");
        }
    }

    public void assertMarketQuotaAvailable(String tenantId) {
        TenantGovernanceProperties.TierLimit limit = limitFor(resolveTier(tenantId));
        long currentCount = tenantMarketRepository.findAllByTenantIdOrderByNameAsc(tenantId).stream()
                .filter(market -> market.getStatus() == MarketStatus.ACTIVE)
                .count();
        if (currentCount >= limit.getMaxMarkets()) {
            throw new ConflictException("Market quota reached for tenant tier.");
        }
    }

    public void assertActiveMembershipQuotaAvailable(String tenantId) {
        TenantGovernanceProperties.TierLimit limit = limitFor(resolveTier(tenantId));
        long currentCount = membershipRepository.findAllByTenantId(tenantId).stream()
                .filter(membership -> membership.getStatus() == MembershipStatus.ACTIVE)
                .count();
        if (currentCount >= limit.getMaxActiveMemberships()) {
            throw new ConflictException("Active membership quota reached for tenant tier.");
        }
    }

    public Optional<TenantEntity> findTenant(String tenantId) {
        return tenantRepository.findById(tenantId);
    }

    private TenantGovernanceProperties.TierLimit limitFor(TenantServiceTier tier) {
        return switch (tier) {
            case INTERNAL -> properties.getQuota().getInternal();
            case ENTERPRISE -> properties.getQuota().getEnterprise();
            case STANDARD -> properties.getQuota().getStandard();
        };
    }

    public record RateLimitPolicy(Duration window, int maxRequests) {
    }
}
