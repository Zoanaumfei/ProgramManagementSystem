package com.oryzem.programmanagementsystem.platform.tenant;

import com.oryzem.programmanagementsystem.modules.projectmanagement.PortfolioResetTenantPort;
import org.springframework.stereotype.Component;

@Component
class TenantPortfolioResetAdapter implements PortfolioResetTenantPort {

    private final OrganizationRepository organizationRepository;

    TenantPortfolioResetAdapter(OrganizationRepository organizationRepository) {
        this.organizationRepository = organizationRepository;
    }

    @Override
    public void clearOrganizations() {
        organizationRepository.deleteAll();
    }
}
