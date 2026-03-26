package com.oryzem.programmanagementsystem.platform.access;

import com.oryzem.programmanagementsystem.platform.authorization.Role;
import com.oryzem.programmanagementsystem.platform.authorization.TenantType;
import com.oryzem.programmanagementsystem.platform.tenant.OrganizationBoundaryResolver;
import com.oryzem.programmanagementsystem.platform.users.domain.ManagedUser;
import com.oryzem.programmanagementsystem.platform.users.domain.UserRepository;
import com.oryzem.programmanagementsystem.platform.users.domain.UserStatus;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
                .orElseThrow(() -> new IllegalStateException(
                        "Membership '%s' does not resolve a tenant type.".formatted(selectedMembership.getId())));

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

    public ResolvedMembershipContext requireActiveContext(ManagedUser user) {
        return resolveActiveContext(user, null)
                .orElseThrow(() -> new IllegalStateException(
                        "User '%s' does not have an active membership context.".formatted(user.id())));
    }

    public Optional<Role> resolvePrimaryRole(ManagedUser user) {
        return resolveActiveContext(user, null)
                .map(ResolvedMembershipContext::roles)
                .map(this::primaryRole);
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

    public Optional<String> resolvePrimaryOrganizationId(ManagedUser user) {
        return resolveActiveContext(user, null)
                .map(ResolvedMembershipContext::activeOrganizationId);
    }

    public Optional<TenantType> resolvePrimaryTenantType(ManagedUser user) {
        return resolveActiveContext(user, null)
                .map(ResolvedMembershipContext::tenantType);
    }

    public Optional<String> resolvePrimaryTenantId(ManagedUser user) {
        return resolveActiveContext(user, null)
                .map(ResolvedMembershipContext::activeTenantId);
    }

    public String resolveTenantBoundaryId(String organizationId) {
        if (!hasText(organizationId)) {
            return null;
        }
        if (tenantRepository.existsById(organizationId)) {
            return organizationId;
        }
        return organizationBoundaryResolver.findBoundary(organizationId)
                .map(OrganizationBoundaryResolver.OrganizationBoundaryView::tenantId)
                .orElse(organizationId);
    }

    public Set<String> equivalentTenantIds(String tenantOrOrganizationId) {
        String canonicalTenantId = canonicalTenantId(tenantOrOrganizationId);
        if (!hasText(canonicalTenantId)) {
            return Set.of();
        }

        LinkedHashSet<String> equivalents = new LinkedHashSet<>();
        equivalents.add(canonicalTenantId);
        tenantRepository.findById(canonicalTenantId)
                .map(TenantEntity::getRootOrganizationId)
                .filter(this::hasText)
                .ifPresent(equivalents::add);
        return Set.copyOf(equivalents);
    }

    public String canonicalTenantId(String tenantOrOrganizationId) {
        if (!hasText(tenantOrOrganizationId)) {
            return null;
        }
        return resolveTenantBoundaryId(tenantOrOrganizationId.trim());
    }

    public Optional<TenantType> resolveTenantType(String tenantOrOrganizationId) {
        String canonicalTenantId = canonicalTenantId(tenantOrOrganizationId);
        if (!hasText(canonicalTenantId)) {
            return Optional.empty();
        }

        return tenantRepository.findById(canonicalTenantId)
                .map(TenantEntity::getTenantType)
                .or(() -> organizationBoundaryResolver.findBoundary(tenantOrOrganizationId)
                        .map(OrganizationBoundaryResolver.OrganizationBoundaryView::tenantType));
    }

    public Set<String> findUserIdsByOrganization(String organizationId) {
        if (!hasText(organizationId)) {
            return Set.of();
        }
        return membershipRepository.findAllByOrganizationId(organizationId).stream()
                .filter(membership -> membership.getStatus() != MembershipStatus.INACTIVE)
                .map(UserMembershipEntity::getUserId)
                .collect(java.util.stream.Collectors.toSet());
    }

    public Set<String> findUserIdsByTenant(String tenantId) {
        if (!hasText(tenantId)) {
            return Set.of();
        }

        Map<String, UserMembershipEntity> activeMembershipsById = new LinkedHashMap<>();
        membershipRepository.findAllByTenantId(canonicalTenantId(tenantId)).stream()
                .filter(membership -> membership.getStatus() != MembershipStatus.INACTIVE)
                .forEach(membership -> activeMembershipsById.put(membership.getId(), membership));
        membershipRepository.findAllByOrganizationIdIn(equivalentTenantIds(tenantId)).stream()
                .filter(membership -> membership.getStatus() != MembershipStatus.INACTIVE)
                .forEach(membership -> activeMembershipsById.putIfAbsent(membership.getId(), membership));
        return activeMembershipsById.values().stream()
                .map(UserMembershipEntity::getUserId)
                .collect(java.util.stream.Collectors.toSet());
    }

    public boolean hasInvitedOrActiveAdmin(String organizationId) {
        if (!hasText(organizationId)) {
            return false;
        }

        Set<String> activeMembershipIds = membershipRepository.findAllByOrganizationId(organizationId).stream()
                .filter(membership -> membership.getStatus() != MembershipStatus.INACTIVE)
                .map(UserMembershipEntity::getId)
                .collect(java.util.stream.Collectors.toSet());
        if (activeMembershipIds.isEmpty()) {
            return false;
        }

        Set<String> adminUserIds = membershipRoleRepository.findAll().stream()
                .filter(role -> activeMembershipIds.contains(role.getMembershipId()))
                .filter(role -> Role.ADMIN.name().equals(role.getRoleCode()))
                .map(MembershipRoleEntity::getMembershipId)
                .flatMap(membershipId -> membershipRepository.findById(membershipId).stream())
                .map(UserMembershipEntity::getUserId)
                .collect(java.util.stream.Collectors.toSet());
        if (adminUserIds.isEmpty()) {
            return false;
        }

        return userRepository.findAll().stream()
                .filter(user -> adminUserIds.contains(user.id()))
                .anyMatch(user -> user.status() == com.oryzem.programmanagementsystem.platform.users.domain.UserStatus.ACTIVE
                        || user.status() == com.oryzem.programmanagementsystem.platform.users.domain.UserStatus.INVITED);
    }

    @Transactional
    public void upsertDefaultMembership(
            String userId,
            String tenantId,
            String organizationId,
            String marketId,
            UserStatus userStatus,
            Set<Role> roles,
            java.time.Instant joinedAt) {
        if (!hasText(userId) || !hasText(tenantId) || !hasText(organizationId)) {
            throw new IllegalArgumentException("Default membership requires userId, tenantId and organizationId.");
        }

        java.time.Instant now = java.time.Instant.now();
        UserMembershipEntity membership = membershipRepository.findByUserIdAndDefaultMembershipTrue(userId)
                .orElseGet(() -> UserMembershipEntity.createDefault(
                        "MBR-" + userId,
                        userId,
                        tenantId,
                        organizationId,
                        marketId,
                        toMembershipStatus(userStatus),
                        joinedAt != null ? joinedAt : now,
                        now));
        membership.synchronizeDefaultContext(
                tenantId,
                organizationId,
                marketId,
                toMembershipStatus(userStatus),
                now);
        membershipRepository.save(membership);

        membershipRoleRepository.deleteByMembershipId(membership.getId());
        if (roles != null) {
            roles.stream()
                    .sorted(Comparator.comparing(Enum::name))
                    .forEach(role -> membershipRoleRepository.save(MembershipRoleEntity.create(
                            "MBRROLE-" + membership.getId() + "-" + role.name(),
                            membership.getId(),
                            role.name())));
        }
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

    private Role primaryRole(Set<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            return null;
        }
        List<Role> precedence = List.of(Role.ADMIN, Role.SUPPORT, Role.MANAGER, Role.AUDITOR, Role.MEMBER);
        return precedence.stream()
                .filter(roles::contains)
                .findFirst()
                .orElseGet(() -> roles.stream().sorted(Comparator.comparing(Enum::name)).findFirst().orElse(null));
    }
}
