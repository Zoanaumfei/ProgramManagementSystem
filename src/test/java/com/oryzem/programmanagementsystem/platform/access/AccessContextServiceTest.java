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

@SpringBootTest(classes = com.oryzem.programmanagementsystem.app.ProgramManagementSystemApplication.class)
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
                Role.MANAGER,
                "tenant-a",
                TenantType.EXTERNAL,
                UserStatus.ACTIVE,
                Instant.parse("2026-03-24T21:00:00Z"),
                null,
                null));

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
        Assertions.assertThat(context.permissions()).contains("portfolio.view", "operations.view");
    }

    @Test
    void shouldResolveRequestedOrganizationHintAcrossMultipleMemberships() {
        ManagedUser saved = userRepository.save(new ManagedUser(
                "USR-MBR-002",
                "context.switch@oryzem.com",
                "subject-membership-002",
                "Context Switch",
                "context.switch@oryzem.com",
                Role.ADMIN,
                "tenant-a",
                TenantType.EXTERNAL,
                UserStatus.ACTIVE,
                Instant.parse("2026-03-24T21:05:00Z"),
                null,
                null));

        organizationBootstrapPort.ensureSeeded(
                "tenant-b",
                "test",
                "Tenant B",
                "TENANT-B",
                TenantType.EXTERNAL,
                null,
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
}
