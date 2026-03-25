package com.oryzem.programmanagementsystem.platform.access;

import com.oryzem.programmanagementsystem.platform.authorization.Role;
import com.oryzem.programmanagementsystem.platform.authorization.TenantType;
import com.oryzem.programmanagementsystem.platform.tenant.OrganizationBoundaryResolver;
import com.oryzem.programmanagementsystem.platform.users.domain.ManagedUser;
import com.oryzem.programmanagementsystem.platform.users.domain.UserRepository;
import com.oryzem.programmanagementsystem.platform.users.domain.UserStatus;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AccessContextService {

    private final UserRepository userRepository;
    private final OrganizationBoundaryResolver organizationBoundaryResolver;
    private final SpringDataTenantJpaRepository tenantRepository;
    private final SpringDataUserMembershipJpaRepository membershipRepository;
    private final SpringDataMembershipRoleJpaRepository membershipRoleRepository;
    private final SpringDataRolePermissionJpaRepository rolePermissionRepository;

    public AccessContextService(
            @Lazy UserRepository userRepository,
            OrganizationBoundaryResolver organizationBoundaryResolver,
            SpringDataTenantJpaRepository tenantRepository,
            SpringDataUserMembershipJpaRepository membershipRepository,
            SpringDataMembershipRoleJpaRepository membershipRoleRepository,
            SpringDataRolePermissionJpaRepository rolePermissionRepository) {
        this.userRepository = userRepository;
        this.organizationBoundaryResolver = organizationBoundaryResolver;
        this.tenantRepository = tenantRepository;
        this.membershipRepository = membershipRepository;
        this.membershipRoleRepository = membershipRoleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
    }

    public Optional<ResolvedMembershipContext> resolveActiveContext(
            String identitySubject,
            String identityUsername,
            String email,
            String requestedContextHint) {
        return resolveUser(identitySubject, identityUsername, email)
                .flatMap(user -> resolveActiveContext(user, requestedContextHint));
    }

    public Optional<ResolvedMembershipContext> resolveActiveContext(ManagedUser user, String requestedContextHint) {
        List<UserMembershipEntity> memberships = membershipRepository.findByUserIdOrderByDefaultMembershipDescJoinedAtAsc(user.id())
                .stream()
                .filter(membership -> membership.getStatus() == MembershipStatus.ACTIVE)
                .toList();
        if (memberships.isEmpty()) {
            return Optional.empty();
        }

        UserMembershipEntity selectedMembership = selectMembership(memberships, requestedContextHint)
                .orElse(memberships.getFirst());
        Set<Role> roles = membershipRoleRepository.findByMembershipId(selectedMembership.getId()).stream()
                .map(MembershipRoleEntity::getRoleCode)
                .map(this::toRole)
                .flatMap(Optional::stream)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        Set<String> permissions = rolePermissionRepository.findByRoleCodeIn(
                        roles.stream().map(Enum::name).toList())
                .stream()
                .map(RolePermissionEntity::getPermissionCode)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        Optional<TenantType> tenantTypeFromOrganization = selectedMembership.getOrganizationId() == null
                ? Optional.empty()
                : organizationBoundaryResolver.findBoundary(selectedMembership.getOrganizationId())
                        .map(OrganizationBoundaryResolver.OrganizationBoundaryView::tenantType);
        TenantType tenantType = tenantRepository.findById(selectedMembership.getTenantId())
                .map(TenantEntity::getTenantType)
                .or(() -> tenantTypeFromOrganization)
                .orElse(user.tenantType());

        return Optional.of(new ResolvedMembershipContext(
                user.id(),
                selectedMembership.getId(),
                selectedMembership.getTenantId(),
                selectedMembership.getOrganizationId(),
                selectedMembership.getMarketId(),
                tenantType,
                Set.copyOf(roles),
                Set.copyOf(permissions)));
    }

    @Transactional
    public void synchronizeDefaultMembership(ManagedUser user) {
        if (user == null || user.id() == null || user.id().isBlank()) {
            return;
        }

        String organizationId = user.tenantId();
        if (organizationId == null || organizationId.isBlank()) {
            return;
        }

        membershipRepository.findByUserIdAndDefaultMembershipTrue(user.id())
                .ifPresentOrElse(
                        membership -> synchronizeNonDestructive(user, membership),
                        () -> synchronizeLegacyDefaultMembership(user));
    }

    @Transactional
    public void synchronizeLegacyDefaultMembership(ManagedUser user) {
        if (user == null || user.id() == null || user.id().isBlank()) {
            return;
        }

        String organizationId = user.tenantId();
        if (organizationId == null || organizationId.isBlank()) {
            return;
        }

        var now = java.time.Instant.now();
        UserMembershipEntity membership = membershipRepository.findByUserIdAndDefaultMembershipTrue(user.id())
                .orElseGet(() -> UserMembershipEntity.createDefault(
                        "MBR-" + user.id(),
                        user.id(),
                        resolveTenantId(organizationId),
                        organizationId,
                        null,
                        toMembershipStatus(user.status()),
                        user.createdAt(),
                        now));
        membership.synchronizeDefaultContext(
                resolveTenantId(organizationId),
                organizationId,
                null,
                toMembershipStatus(user.status()),
                now);
        membershipRepository.save(membership);
        replaceLegacyCompatibilityRoles(membership.getId(), user.role());
    }

    @Transactional
    public void deleteMembershipsForUser(String userId) {
        if (userId == null || userId.isBlank()) {
            return;
        }

        membershipRepository.findByUserIdOrderByDefaultMembershipDescJoinedAtAsc(userId)
                .forEach(membership -> membershipRoleRepository.deleteByMembershipId(membership.getId()));
        membershipRepository.deleteByUserId(userId);
    }

    public ManagedUser hydrateLegacyCompatibilityView(ManagedUser user) {
        if (user == null) {
            return null;
        }

        return resolveActiveContext(user, null)
                .map(context -> user.withUpdatedDetails(
                        user.displayName(),
                        user.email(),
                        preferredLegacyRole(context.roles(), user.role()),
                        context.activeOrganizationId() != null ? context.activeOrganizationId() : user.tenantId(),
                        context.tenantType() != null ? context.tenantType() : user.tenantType()))
                .orElse(user);
    }

    private Optional<ManagedUser> resolveUser(String identitySubject, String identityUsername, String email) {
        if (hasText(identitySubject)) {
            Optional<ManagedUser> bySubject = userRepository.findByIdentitySubject(identitySubject);
            if (bySubject.isPresent()) {
                return bySubject;
            }
        }
        if (hasText(identityUsername)) {
            Optional<ManagedUser> byUsername = userRepository.findByIdentityUsername(identityUsername);
            if (byUsername.isPresent()) {
                return byUsername;
            }
        }
        if (hasText(email)) {
            return userRepository.findByEmailIgnoreCase(email.toLowerCase(Locale.ROOT));
        }
        return Optional.empty();
    }

    private Optional<UserMembershipEntity> selectMembership(List<UserMembershipEntity> memberships, String requestedContextHint) {
        if (hasText(requestedContextHint)) {
            Optional<UserMembershipEntity> exactMatch = memberships.stream()
                    .filter(membership -> requestedContextHint.equals(membership.getId())
                            || requestedContextHint.equals(membership.getOrganizationId())
                            || requestedContextHint.equals(membership.getTenantId())
                            || requestedContextHint.equals(membership.getMarketId()))
                    .min(Comparator.comparing(UserMembershipEntity::isDefaultMembership).reversed());
            if (exactMatch.isPresent()) {
                return exactMatch;
            }
        }

        return memberships.stream()
                .filter(UserMembershipEntity::isDefaultMembership)
                .findFirst();
    }

    private MembershipStatus toMembershipStatus(UserStatus status) {
        return status == UserStatus.INACTIVE ? MembershipStatus.INACTIVE : MembershipStatus.ACTIVE;
    }

    private void synchronizeNonDestructive(ManagedUser user, UserMembershipEntity membership) {
        var now = java.time.Instant.now();
        String resolvedOrganizationId = hasText(membership.getOrganizationId()) ? membership.getOrganizationId() : user.tenantId();
        String resolvedTenantId = hasText(membership.getTenantId()) ? membership.getTenantId() : resolveTenantId(resolvedOrganizationId);
        membership.synchronizeDefaultContext(
                resolvedTenantId,
                resolvedOrganizationId,
                membership.getMarketId(),
                toMembershipStatus(user.status()),
                now);
        membershipRepository.save(membership);

        if (membershipRoleRepository.findByMembershipId(membership.getId()).isEmpty() && user.role() != null) {
            membershipRoleRepository.save(MembershipRoleEntity.create(
                    "MBRROLE-" + membership.getId() + "-" + user.role().name(),
                    membership.getId(),
                    user.role().name()));
        }
    }

    private void replaceLegacyCompatibilityRoles(String membershipId, Role role) {
        membershipRoleRepository.deleteByMembershipId(membershipId);
        if (role != null) {
            membershipRoleRepository.save(MembershipRoleEntity.create(
                    "MBRROLE-" + membershipId + "-" + role.name(),
                    membershipId,
                    role.name()));
        }
    }

    private String resolveTenantId(String organizationId) {
        return organizationBoundaryResolver.findBoundary(organizationId)
                .map(OrganizationBoundaryResolver.OrganizationBoundaryView::tenantId)
                .orElseGet(() -> "TEN-" + organizationId);
    }

    private Role preferredLegacyRole(Set<Role> roles, Role fallback) {
        if (roles == null || roles.isEmpty()) {
            return fallback;
        }
        List<Role> precedence = List.of(Role.ADMIN, Role.SUPPORT, Role.MANAGER, Role.AUDITOR, Role.MEMBER);
        return precedence.stream()
                .filter(roles::contains)
                .findFirst()
                .orElse(fallback != null ? fallback : roles.stream().sorted().findFirst().orElse(null));
    }

    private Optional<Role> toRole(String roleCode) {
        if (!hasText(roleCode)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Role.valueOf(roleCode.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
