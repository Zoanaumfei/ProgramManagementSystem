package com.oryzem.programmanagementsystem.platform.tenant;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface OrganizationRelationshipRepository extends JpaRepository<OrganizationRelationshipEntity, String> {

    List<OrganizationRelationshipEntity> findAllByRelationshipTypeAndStatus(
            OrganizationRelationshipType relationshipType,
            OrganizationRelationshipStatus status);

    List<OrganizationRelationshipEntity> findAllBySourceOrganizationIdAndStatus(
            String sourceOrganizationId,
            OrganizationRelationshipStatus status);

    List<OrganizationRelationshipEntity> findAllBySourceOrganizationIdAndRelationshipTypeAndStatus(
            String sourceOrganizationId,
            OrganizationRelationshipType relationshipType,
            OrganizationRelationshipStatus status);
}
