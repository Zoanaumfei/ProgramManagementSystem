package com.oryzem.programmanagementsystem.platform.access;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.multitenancy")
public class TenantGovernanceProperties {

    private final Offboarding offboarding = new Offboarding();
    private final RateLimit rateLimit = new RateLimit();
    private final Quota quota = new Quota();

    public Offboarding getOffboarding() {
        return offboarding;
    }

    public RateLimit getRateLimit() {
        return rateLimit;
    }

    public Quota getQuota() {
        return quota;
    }

    public static class Offboarding {
        private int retentionDays = 30;

        public int getRetentionDays() {
            return retentionDays;
        }

        public void setRetentionDays(int retentionDays) {
            this.retentionDays = retentionDays;
        }
    }

    public static class RateLimit {
        private int windowSeconds = 60;
        private int internalMaxRequests = 240;
        private int standardMaxRequests = 120;
        private int enterpriseMaxRequests = 300;

        public int getWindowSeconds() {
            return windowSeconds;
        }

        public void setWindowSeconds(int windowSeconds) {
            this.windowSeconds = windowSeconds;
        }

        public int getInternalMaxRequests() {
            return internalMaxRequests;
        }

        public void setInternalMaxRequests(int internalMaxRequests) {
            this.internalMaxRequests = internalMaxRequests;
        }

        public int getStandardMaxRequests() {
            return standardMaxRequests;
        }

        public void setStandardMaxRequests(int standardMaxRequests) {
            this.standardMaxRequests = standardMaxRequests;
        }

        public int getEnterpriseMaxRequests() {
            return enterpriseMaxRequests;
        }

        public void setEnterpriseMaxRequests(int enterpriseMaxRequests) {
            this.enterpriseMaxRequests = enterpriseMaxRequests;
        }
    }

    public static class Quota {
        private final TierLimit internal = new TierLimit(1000, 1000, 10000);
        private final TierLimit standard = new TierLimit(20, 10, 250);
        private final TierLimit enterprise = new TierLimit(200, 50, 5000);

        public TierLimit getInternal() {
            return internal;
        }

        public TierLimit getStandard() {
            return standard;
        }

        public TierLimit getEnterprise() {
            return enterprise;
        }
    }

    public static class TierLimit {
        private int maxOrganizations;
        private int maxMarkets;
        private int maxActiveMemberships;

        public TierLimit() {
        }

        public TierLimit(int maxOrganizations, int maxMarkets, int maxActiveMemberships) {
            this.maxOrganizations = maxOrganizations;
            this.maxMarkets = maxMarkets;
            this.maxActiveMemberships = maxActiveMemberships;
        }

        public int getMaxOrganizations() {
            return maxOrganizations;
        }

        public void setMaxOrganizations(int maxOrganizations) {
            this.maxOrganizations = maxOrganizations;
        }

        public int getMaxMarkets() {
            return maxMarkets;
        }

        public void setMaxMarkets(int maxMarkets) {
            this.maxMarkets = maxMarkets;
        }

        public int getMaxActiveMemberships() {
            return maxActiveMemberships;
        }

        public void setMaxActiveMemberships(int maxActiveMemberships) {
            this.maxActiveMemberships = maxActiveMemberships;
        }
    }
}
