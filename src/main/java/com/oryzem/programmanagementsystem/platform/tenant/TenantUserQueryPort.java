package com.oryzem.programmanagementsystem.platform.tenant;

import java.util.Map;
import java.util.Set;

public interface TenantUserQueryPort {

    boolean hasInvitedOrActiveAdmin(String organizationId);

    boolean hasInvitedOrActiveUsers(String organizationId);

    Map<String, OrganizationUserStats> summarizeByOrganizationIds(Set<String> organizationIds);

    record OrganizationUserStats(
            int invitedCount,
            int activeCount,
            int inactiveCount,
            boolean hasInvitedOrActiveAdmin) {

        public static OrganizationUserStats empty() {
            return new OrganizationUserStats(0, 0, 0, false);
        }
    }
}
