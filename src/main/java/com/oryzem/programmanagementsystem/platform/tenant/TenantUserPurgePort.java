package com.oryzem.programmanagementsystem.platform.tenant;

import java.util.Set;

public interface TenantUserPurgePort {

    int purgeUsersByOrganizationIds(Set<String> organizationIds);

    OffboardingSummary offboardUsersByOrganizationIds(Set<String> organizationIds, java.time.Instant retentionUntil);

    record OffboardingSummary(
            int affectedUsers,
            int disabledUsers,
            int offboardedMemberships) {
    }
}
