package com.oryzem.programmanagementsystem.platform.tenant;

import com.oryzem.programmanagementsystem.platform.authorization.TenantType;

public interface OrganizationBootstrapPort {

    OrganizationLookup.OrganizationView ensureSeeded(
            String organizationId,
            String actor,
            String name,
            String code,
            String cnpj,
            TenantType tenantType,
            boolean active);
}
