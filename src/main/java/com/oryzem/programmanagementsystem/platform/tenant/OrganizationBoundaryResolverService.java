package com.oryzem.programmanagementsystem.platform.tenant;

import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class OrganizationBoundaryResolverService implements OrganizationBoundaryResolver {

    private final OrganizationRepository organizationRepository;

    OrganizationBoundaryResolverService(OrganizationRepository organizationRepository) {
        this.organizationRepository = organizationRepository;
    }

    @Override
    public Optional<OrganizationBoundaryView> findBoundary(String organizationId) {
        return organizationRepository.findById(organizationId)
                .map(organization -> new OrganizationBoundaryView(
                        organization.getId(),
                        organization.getTenantId(),
                        organization.getMarketId(),
                        organization.getTenantType()));
    }
}
