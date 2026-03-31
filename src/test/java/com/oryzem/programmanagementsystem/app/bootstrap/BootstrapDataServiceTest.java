package com.oryzem.programmanagementsystem.app.bootstrap;

import com.oryzem.programmanagementsystem.platform.access.AccessContextResetService;
import com.oryzem.programmanagementsystem.platform.access.AccessContextService;
import com.oryzem.programmanagementsystem.platform.audit.AuditTrailService;
import com.oryzem.programmanagementsystem.platform.authorization.Role;
import com.oryzem.programmanagementsystem.platform.authorization.TenantType;
import com.oryzem.programmanagementsystem.platform.tenant.OrganizationBootstrapPort;
import com.oryzem.programmanagementsystem.platform.tenant.OrganizationLookup;
import com.oryzem.programmanagementsystem.platform.tenant.OrganizationResetPort;
import com.oryzem.programmanagementsystem.platform.users.domain.ManagedUser;
import com.oryzem.programmanagementsystem.platform.users.domain.UserIdentityGateway;
import com.oryzem.programmanagementsystem.platform.users.domain.UserRepository;
import com.oryzem.programmanagementsystem.platform.users.domain.UserStatus;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        classes = com.oryzem.programmanagementsystem.app.ProgramManagementSystemApplication.class,
        properties = "app.bootstrap.seed-data=false")
class BootstrapDataServiceTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditTrailService auditTrailService;

    @Autowired
    private AccessContextResetService accessContextResetService;

    @Autowired
    private AccessContextService accessContextService;

    @Autowired
    private OrganizationBootstrapPort organizationBootstrapPort;

    @Autowired
    private OrganizationResetPort organizationResetPort;

    @Autowired
    private OrganizationLookup organizationLookup;

    @Autowired
    private UserIdentityGateway userIdentityGateway;

    @BeforeEach
    void setUp() {
        resetWithProperties(new BootstrapProperties(false, new BootstrapProperties.InternalAdminProperties(false, null, null, false, false, null, null)));
        clearStubIdentityGatewayIfPresent();
    }

    @Test
    void shouldEnsureInternalCoreEvenWhenSeedDataIsDisabled() {
        OrganizationLookup.OrganizationView internalCore = organizationLookup.getRequired("internal-core");

        Assertions.assertThat(internalCore.id()).isEqualTo("internal-core");
        Assertions.assertThat(internalCore.tenantType()).isEqualTo(TenantType.INTERNAL);
        Assertions.assertThat(internalCore.active()).isTrue();
    }

    @Test
    void shouldBootstrapInternalAdminWhenExplicitlyEnabled() {
        resetWithProperties(new BootstrapProperties(
                false,
                new BootstrapProperties.InternalAdminProperties(
                        true,
                        "bootstrap.admin@oryzem.com",
                        "Bootstrap Admin",
                        false,
                        false,
                        "PermanentPassword123!",
                        "TempPassword123!")));

        ManagedUser createdUser = userRepository.findByEmailIgnoreCase("bootstrap.admin@oryzem.com").orElseThrow();
        Assertions.assertThat(createdUser.status()).isEqualTo(UserStatus.ACTIVE);
        Assertions.assertThat(accessContextService.resolvePrimaryRole(createdUser)).contains(Role.ADMIN);
        Assertions.assertThat(accessContextService.resolvePrimaryOrganizationId(createdUser)).contains("internal-core");
        Assertions.assertThat(accessContextService.resolvePrimaryTenantType(createdUser)).contains(TenantType.INTERNAL);
        Assertions.assertThat(userIdentityGateway.identityExists(createdUser)).isTrue();
        assertBootstrapRoles(Role.ADMIN, Role.SUPPORT, Role.AUDITOR);
    }

    @Test
    void shouldNotBootstrapDuplicateInternalAdminWhenOneAlreadyExists() {
        BootstrapProperties properties = new BootstrapProperties(
                false,
                new BootstrapProperties.InternalAdminProperties(
                        true,
                        "bootstrap.admin@oryzem.com",
                        "Bootstrap Admin",
                        false,
                        false,
                        "PermanentPassword123!",
                        "TempPassword123!"));

        BootstrapDataService service = new BootstrapDataService(
                userRepository,
                auditTrailService,
                accessContextService,
                accessContextResetService,
                organizationBootstrapPort,
                organizationResetPort,
                userIdentityGateway,
                properties);

        service.seedIfEmpty();
        service.seedIfEmpty();

        Assertions.assertThat(userRepository.findAll())
                .filteredOn(user -> accessContextService.resolvePrimaryOrganizationId(user).orElse("").equals("internal-core")
                        && accessContextService.resolvePrimaryRole(user).orElse(null) == Role.ADMIN)
                .hasSize(1);
    }

    @Test
    void shouldCreateConfiguredBootstrapAdminEvenWhenAnotherInternalAdminAlreadyExists() {
        resetWithProperties(new BootstrapProperties(
                false,
                new BootstrapProperties.InternalAdminProperties(
                        true,
                        "bootstrap.admin@oryzem.com",
                        "Bootstrap Admin",
                        false,
                        false,
                        "PermanentPassword123!",
                        "TempPassword123!")));

        BootstrapDataService service = new BootstrapDataService(
                userRepository,
                auditTrailService,
                accessContextService,
                accessContextResetService,
                organizationBootstrapPort,
                organizationResetPort,
                userIdentityGateway,
                new BootstrapProperties(
                        false,
                        new BootstrapProperties.InternalAdminProperties(
                                true,
                                "recovery.admin@oryzem.com",
                                "Recovery Admin",
                                false,
                                false,
                                "PermanentPassword123!",
                                "TempPassword123!")));

        service.seedIfEmpty();

        Assertions.assertThat(userRepository.findAll())
                .filteredOn(user -> accessContextService.resolvePrimaryOrganizationId(user).orElse("").equals("internal-core")
                        && accessContextService.resolvePrimaryRole(user).orElse(null) == Role.ADMIN)
                .hasSize(2)
                .extracting(ManagedUser::email)
                .contains("bootstrap.admin@oryzem.com", "recovery.admin@oryzem.com");
    }

    @Test
    void shouldPruneOtherInternalUsersWhenExplicitlyEnabled() {
        resetWithProperties(new BootstrapProperties(
                false,
                new BootstrapProperties.InternalAdminProperties(
                        true,
                        "vanderson.verza@gmail.com",
                        "Vanderson Verza",
                        false,
                        false,
                        "PermanentPassword123!",
                        "TempPassword123!")));

        userRepository.save(new ManagedUser(
                "USR-LEGACY-ADMIN-001",
                "admin@oryzem.com",
                null,
                "Legacy Admin",
                "admin@oryzem.com",
                UserStatus.ACTIVE,
                java.time.Instant.now(),
                null,
                null));
        accessContextService.upsertDefaultMembership(
                "USR-LEGACY-ADMIN-001",
                accessContextService.resolveTenantBoundaryId("internal-core"),
                "internal-core",
                null,
                UserStatus.ACTIVE,
                java.util.Set.of(Role.ADMIN),
                java.time.Instant.now());

        BootstrapDataService service = new BootstrapDataService(
                userRepository,
                auditTrailService,
                accessContextService,
                accessContextResetService,
                organizationBootstrapPort,
                organizationResetPort,
                userIdentityGateway,
                new BootstrapProperties(
                        false,
                        new BootstrapProperties.InternalAdminProperties(
                                true,
                                "vanderson.verza@gmail.com",
                                "Vanderson Verza",
                                true,
                                false,
                                "PermanentPassword123!",
                                "TempPassword123!")));

        service.seedIfEmpty();

        Assertions.assertThat(userRepository.findByEmailIgnoreCase("vanderson.verza@gmail.com")).isPresent();
        Assertions.assertThat(userRepository.findByEmailIgnoreCase("admin@oryzem.com")).isNotPresent();
    }

    @Test
    void shouldPruneToInternalCoreSkeletonWhenExplicitlyEnabled() {
        resetWithProperties(new BootstrapProperties(true, new BootstrapProperties.InternalAdminProperties(false, null, null, false, false, null, null)));

        Assertions.assertThat(organizationLookup.findById("tenant-a")).isPresent();
        Assertions.assertThat(userRepository.findByEmailIgnoreCase("admin.a@tenant.com")).isPresent();

        BootstrapDataService service = new BootstrapDataService(
                userRepository,
                auditTrailService,
                accessContextService,
                accessContextResetService,
                organizationBootstrapPort,
                organizationResetPort,
                userIdentityGateway,
                new BootstrapProperties(
                        true,
                        new BootstrapProperties.InternalAdminProperties(
                                true,
                                "vanderson.verza@gmail.com",
                                "Vanderson Verza",
                                true,
                                true,
                                "PermanentPassword123!",
                                "TempPassword123!")));

        service.seedIfEmpty();

        Assertions.assertThat(organizationLookup.findById("internal-core")).isPresent();
        Assertions.assertThat(organizationLookup.findById("tenant-a")).isNotPresent();
        Assertions.assertThat(organizationLookup.findById("tenant-b")).isNotPresent();
        Assertions.assertThat(userRepository.findAll())
                .extracting(ManagedUser::email)
                .containsExactly("vanderson.verza@gmail.com");
        Assertions.assertThat(accessContextService.resolvePrimaryOrganizationId(
                        userRepository.findByEmailIgnoreCase("vanderson.verza@gmail.com").orElseThrow()))
                .contains("internal-core");
        assertBootstrapRoles(Role.ADMIN, Role.SUPPORT, Role.AUDITOR);
    }

    private void resetWithProperties(BootstrapProperties properties) {
        BootstrapDataService service = new BootstrapDataService(
                userRepository,
                auditTrailService,
                accessContextService,
                accessContextResetService,
                organizationBootstrapPort,
                organizationResetPort,
                userIdentityGateway,
                properties);
        service.reset();
    }

    private void clearStubIdentityGatewayIfPresent() {
        try {
            java.lang.reflect.Method clearMethod = userIdentityGateway.getClass().getDeclaredMethod("clear");
            clearMethod.setAccessible(true);
            clearMethod.invoke(userIdentityGateway);
        } catch (NoSuchMethodException ignored) {
            // Non-stub gateway: nothing to clear.
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to clear stub user identity gateway for test setup.", exception);
        }
    }

    @SuppressWarnings("unchecked")
    private void assertBootstrapRoles(Role... expectedRoles) {
        try {
            java.lang.reflect.Method groupsMethod = userIdentityGateway.getClass().getDeclaredMethod("bootstrapRoles");
            groupsMethod.setAccessible(true);
            java.util.Set<Role> actualRoles = (java.util.Set<Role>) groupsMethod.invoke(userIdentityGateway);
            Assertions.assertThat(actualRoles).containsExactlyInAnyOrder(expectedRoles);
        } catch (NoSuchMethodException ignored) {
            // Non-stub gateway: role tracking is not available.
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to inspect stub bootstrap roles for test verification.", exception);
        }
    }
}
