package com.oryzem.programmanagementsystem.platform.tenant;

import com.oryzem.programmanagementsystem.platform.authorization.Action;
import com.oryzem.programmanagementsystem.platform.authorization.AppModule;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.authorization.AuthorizationContext;
import com.oryzem.programmanagementsystem.platform.authorization.AuthorizationDecision;
import com.oryzem.programmanagementsystem.platform.authorization.AuthorizationService;
import com.oryzem.programmanagementsystem.platform.authorization.Role;
import com.oryzem.programmanagementsystem.platform.authorization.TenantType;
import com.oryzem.programmanagementsystem.platform.shared.ResourceNotFoundException;
import java.time.Instant;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class OrganizationRelationshipService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationRelationshipRepository relationshipRepository;
    private final AuthorizationService authorizationService;
    private final OrganizationAccessService accessService;

    OrganizationRelationshipService(
            OrganizationRepository organizationRepository,
            OrganizationRelationshipRepository relationshipRepository,
            AuthorizationService authorizationService,
            OrganizationAccessService accessService) {
        this.organizationRepository = organizationRepository;
        this.relationshipRepository = relationshipRepository;
        this.authorizationService = authorizationService;
        this.accessService = accessService;
    }

    @Transactional(readOnly = true)
    List<OrganizationRelationshipResponse> listRelationships(AuthenticatedUser actor, String organizationId) {
        OrganizationEntity source = findManagedOrganization(organizationId);
        accessService.assertCanViewOrganization(actor, source);
        return relationshipRepository.findAllBySourceOrganizationIdAndStatus(
                        source.getId(),
                        OrganizationRelationshipStatus.ACTIVE).stream()
                .map(OrganizationRelationshipResponse::from)
                .toList();
    }

    OrganizationRelationshipResponse createRelationship(
            AuthenticatedUser actor,
            String organizationId,
            CreateOrganizationRelationshipRequest request) {
        OrganizationEntity source = findManagedOrganization(organizationId);
        OrganizationEntity target = findManagedOrganization(request.targetOrganizationId());
        authorizeRelationshipMutation(actor, source, target);

        OrganizationRelationshipEntity saved = relationshipRepository.save(OrganizationRelationshipEntity.create(
                OrganizationIds.newId("REL"),
                actor.username(),
                source.getId(),
                target.getId(),
                request.relationshipType(),
                OrganizationRelationshipStatus.ACTIVE,
                Instant.now(),
                Instant.now()));
        return OrganizationRelationshipResponse.from(saved);
    }

    OrganizationRelationshipResponse inactivateRelationship(
            AuthenticatedUser actor,
            String organizationId,
            String relationshipId) {
        OrganizationEntity source = findManagedOrganization(organizationId);
        OrganizationRelationshipEntity relationship = relationshipRepository.findById(relationshipId)
                .orElseThrow(() -> new ResourceNotFoundException("OrganizationRelationship", relationshipId));
        if (!source.getId().equals(relationship.getSourceOrganizationId())) {
            throw new ResourceNotFoundException("OrganizationRelationship", relationshipId);
        }
        OrganizationEntity target = findManagedOrganization(relationship.getTargetOrganizationId());
        authorizeRelationshipMutation(actor, source, target);

        relationship.setStatus(OrganizationRelationshipStatus.INACTIVE);
        relationship.touch(actor.username());
        OrganizationRelationshipEntity saved = relationshipRepository.save(relationship);
        return OrganizationRelationshipResponse.from(saved);
    }

    private void authorizeRelationshipMutation(
            AuthenticatedUser actor,
            OrganizationEntity source,
            OrganizationEntity target) {
        AuthorizationDecision decision = authorizationService.decide(
                actor,
                AuthorizationContext.builder(AppModule.TENANT, Action.CREATE)
                        .resourceTenantId(source.getTenantId())
                        .resourceTenantType(source.getTenantType())
                        .build());
        if (!decision.allowed()) {
            throw new AccessDeniedException(decision.reason());
        }

        if (actor.hasRole(Role.ADMIN) && actor.tenantType() == TenantType.INTERNAL) {
            return;
        }

        if (actor.hasRole(Role.ADMIN) && actor.tenantType() == TenantType.EXTERNAL) {
            if (actor.organizationId() == null || !accessService.visibleOrganizationIds(actor, false).contains(source.getId())) {
                throw new AccessDeniedException("Organization is outside the manageable relationship scope for the authenticated user.");
            }
            if (!source.getTenantId().equals(actor.tenantId()) || !target.getTenantId().equals(actor.tenantId())) {
                throw new AccessDeniedException("Relationship creation is restricted to the active tenant.");
            }
            return;
        }

        throw new AccessDeniedException("Only admins can manage organization relationships.");
    }

    private OrganizationEntity findManagedOrganization(String organizationId) {
        OrganizationEntity organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", organizationId));
        if (organization.getTenantType() != TenantType.EXTERNAL) {
            throw new ResourceNotFoundException("Organization", organizationId);
        }
        return organization;
    }
}
