package com.oryzem.programmanagementsystem.platform.tenant;

import java.util.Set;

public interface TenantUserPurgePort {

    int purgeUsersByOrganizationIds(Set<String> organizationIds);
}
