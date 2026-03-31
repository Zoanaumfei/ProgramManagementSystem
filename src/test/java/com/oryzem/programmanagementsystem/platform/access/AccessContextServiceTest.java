package com.oryzem.programmanagementsystem.platform.access;

import com.oryzem.programmanagementsystem.app.bootstrap.BootstrapDataService;
import com.oryzem.programmanagementsystem.platform.authorization.Role;
import com.oryzem.programmanagementsystem.platform.authorization.TenantType;
import com.oryzem.programmanagementsystem.platform.tenant.OrganizationBootstrapPort;
import com.oryzem.programmanagementsystem.platform.users.domain.ManagedUser;
import com.oryzem.programmanagementsystem.platform.users.domain.UserRepository;
import com.oryzem.programmanagementsystem.platform.users.domain.UserStatus;
import java.time.Instant;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = com.oryzem.programmanagementsystem.app.ProgramManagementSystemApplication.class)
@Transactional
class AccessContextServiceTest {

    @Autowired
    private AccessContextService accessContextService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationBootstrapPort organizationBootstrapPort;

    @Autowired
    private SpringDataUserMembershipJpaRepository membershipRepository;

    @Autowired
    private SpringDataMembershipRoleJpaRepository membershipRoleRepository;

    @Autowired
    private BootstrapDataService bootstrapDataService;

    @BeforeEach
    void setUp() {
        bootstrapDataService.reset();
    }

    @Test
    void shouldProvisionDefaultMembershipWhenLegacyUserIsSaved() {
        ManagedUser saved = userRepository.save(new ManagedUser(
                "USR-MBR-001",
                "membership.user@oryzem.com",
                "subject-membership-001",
                "Membership User",
                "membership.user@oryzem.com",
                UserStatus.ACTIVE,
                Instant.parse("2026-03-24T21:00:00Z"),
                null,
                null));
        accessContextService.upsertDefaultMembership(
                saved.id(),
                "TEN-tenant-a",
                "tenant-a",
                null,
                saved.status(),
                java.util.Set.of(Role.MANAGER),
                saved.createdAt());

        ResolvedMembershipContext context = accessContextService.resolveActiveContext(
                        "subject-membership-001",
                        "membership.user@oryzem.com",
                        "membership.user@oryzem.com",
                        null)
                .orElseThrow();

        Assertions.assertThat(context.userId()).isEqualTo(saved.id());
        Assertions.assertThat(context.activeTenantId()).isEqualTo("TEN-tenant-a");
        Assertions.assertThat(context.activeOrganizationId()).isEqualTo("tenant-a");
        Assertions.assertThat(context.roles()).containsExactly(Role.MANAGER);
        Assertions.assertThat(context.permissions())
                .contains("audit.view", "support.view")
                .allMatch(permission -> permission.startsWith("audit.") || permission.startsWith("support."));
    }

    @Test
    void shouldResolveRequestedOrganizationHintAcrossMultipleMemberships() {
        ManagedUser saved = userRepository.save(new ManagedUser(
                "USR-MBR-002",
                "context.switch@oryzem.com",
                "subject-membership-002",
                "Context Switch",
                "context.switch@oryzem.com",
                UserStatus.ACTIVE,
                Instant.parse("2026-03-24T21:05:00Z"),
                null,
                null));
        accessContextService.upsertDefaultMembership(
                saved.id(),
                "TEN-tenant-a",
                "tenant-a",
                null,
                saved.status(),
                java.util.Set.of(Role.ADMIN),
                saved.createdAt());

        organizationBootstrapPort.ensureSeeded(
                "tenant-b",
                "test",
                "Tenant B",
                "98765432000198",
                TenantType.EXTERNAL,
                true);

        membershipRepository.save(UserMembershipEntity.create(
                "MBR-" + saved.id() + "-tenant-b",
                saved.id(),
                "TEN-tenant-b",
                "tenant-b",
                null,
                MembershipStatus.ACTIVE,
                false,
                Instant.parse("2026-03-24T21:06:00Z"),
                Instant.parse("2026-03-24T21:06:00Z")));
        membershipRoleRepository.save(MembershipRoleEntity.create(
                "MBRROLE-" + saved.id() + "-tenant-b-SUPPORT",
                "MBR-" + saved.id() + "-tenant-b",
                Role.SUPPORT.name()));

        ResolvedMembershipContext defaultContext = accessContextService.resolveActiveContext(
                        "subject-membership-002",
                        "context.switch@oryzem.com",
                        "context.switch@oryzem.com",
                        null)
                .orElseThrow();
        ResolvedMembershipContext hintedContext = accessContextService.resolveActiveContext(
                        "subject-membership-002",
                        "context.switch@oryzem.com",
                        "context.switch@oryzem.com",
                        "tenant-b")
                .orElseThrow();

        Assertions.assertThat(defaultContext.activeOrganizationId()).isEqualTo("tenant-a");
        Assertions.assertThat(defaultContext.roles()).containsExactly(Role.ADMIN);
        Assertions.assertThat(hintedContext.activeOrganizationId()).isEqualTo("tenant-b");
        Assertions.assertThat(hintedContext.activeTenantId()).isEqualTo("TEN-tenant-b");
        Assertions.assertThat(hintedContext.roles()).containsExactly(Role.SUPPORT);
        Assertions.assertThat(hintedContext.membershipId()).isEqualTo("MBR-" + saved.id() + "-tenant-b");
    }

    @Test
    void shouldPreserveMembershipManagedContextDuringProfileOnlyUserSaves() {
        ManagedUser saved = userRepository.save(new ManagedUser(
                "USR-MBR-003",
                "compat.user@oryzem.com",
                null,
                "Compatibility User",
                "compat.user@oryzem.com",
                UserStatus.ACTIVE,
                Instant.parse("2026-03-24T21:10:00Z"),
                null,
                null));
        accessContextService.upsertDefaultMembership(
                saved.id(),
                "TEN-tenant-a",
                "tenant-a",
                null,
                saved.status(),
                java.util.Set.of(Role.MEMBER),
                saved.createdAt());

        UserMembershipEntity membership = membershipRepository.findByUserIdAndDefaultMembershipTrue(saved.id()).orElseThrow();
        membership.updateContext(
                "TEN-tenant-b",
                "tenant-b",
                null,
                MembershipStatus.ACTIVE,
                true,
                Instant.parse("2026-03-24T21:11:00Z"));
        membershipRepository.save(membership);
        membershipRoleRepository.deleteByMembershipId(membership.getId());
        membershipRoleRepository.save(MembershipRoleEntity.create(
                "MBRROLE-" + saved.id() + "-ADMIN",
                membership.getId(),
                Role.ADMIN.name()));
        membershipRoleRepository.save(MembershipRoleEntity.create(
                "MBRROLE-" + saved.id() + "-SUPPORT",
                membership.getId(),
                Role.SUPPORT.name()));

        userRepository.save(saved.withIdentitySubject("subject-membership-003"));

        ResolvedMembershipContext context = accessContextService.resolveActiveContext(
                        "subject-membership-003",
                        "compat.user@oryzem.com",
                        "compat.user@oryzem.com",
                        null)
                .orElseThrow();

        Assertions.assertThat(context.activeOrganizationId()).isEqualTo("tenant-b");
        Assertions.assertThat(context.roles()).containsExactlyInAnyOrder(Role.ADMIN, Role.SUPPORT);
    }

    @Test
    void shouldUpsertExplicitDefaultMembershipContext() {
        ManagedUser saved = userRepository.save(new ManagedUser(
                "USR-MBR-004",
                "legacy.sync@oryzem.com",
                "subject-membership-004",
                "Legacy Sync",
                "legacy.sync@oryzem.com",
                UserStatus.ACTIVE,
                Instant.parse("2026-03-24T21:15:00Z"),
                null,
                null));
        accessContextService.upsertDefaultMembership(
                saved.id(),
                "TEN-tenant-b",
                "tenant-b",
                null,
                saved.status(),
                java.util.Set.of(Role.ADMIN),
                saved.createdAt());
        ResolvedMembershipContext context = accessContextService.resolveActiveContext(
                        "subject-membership-004",
                        "legacy.sync@oryzem.com",
                        "legacy.sync@oryzem.com",
                        null)
                .orElseThrow();

        Assertions.assertThat(context.activeTenantId()).isEqualTo("TEN-tenant-b");
        Assertions.assertThat(context.activeOrganizationId()).isEqualTo("tenant-b");
        Assertions.assertThat(context.roles()).containsExactly(Role.ADMIN);
    }
}

