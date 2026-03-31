package com.oryzem.programmanagementsystem.platform.tenant;

import java.util.Collection;
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

    java.util.Optional<OrganizationRelationshipEntity> findBySourceOrganizationIdAndTargetOrganizationIdAndRelationshipType(
            String sourceOrganizationId,
            String targetOrganizationId,
            OrganizationRelationshipType relationshipType);

    boolean existsBySourceOrganizationIdAndLocalOrganizationCodeIgnoreCase(
            String sourceOrganizationId,
            String localOrganizationCode);

    boolean existsBySourceOrganizationIdAndLocalOrganizationCodeIgnoreCaseAndIdNot(
            String sourceOrganizationId,
            String localOrganizationCode,
            String id);

    long deleteBySourceOrganizationIdInOrTargetOrganizationIdIn(
            Collection<String> sourceOrganizationIds,
            Collection<String> targetOrganizationIds);
}
