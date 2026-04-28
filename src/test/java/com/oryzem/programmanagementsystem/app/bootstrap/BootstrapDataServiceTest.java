package com.oryzem.programmanagementsystem.app.bootstrap;

import com.oryzem.programmanagementsystem.platform.access.AccessContextResetService;
import com.oryzem.programmanagementsystem.platform.access.AccessContextService;
import com.oryzem.programmanagementsystem.platform.audit.AuditTrailService;
import com.oryzem.programmanagementsystem.platform.authorization.Role;
import com.oryzem.programmanagementsystem.platform.authorization.TenantType;
import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentStorage;
import com.oryzem.programmanagementsystem.modules.documentmanagement.support.InMemoryDocumentStorageStub;
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
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(
        classes = {
                com.oryzem.programmanagementsystem.app.ProgramManagementSystemApplication.class,
                BootstrapDataServiceTest.DocumentStorageTestConfig.class
        },
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

    @Autowired
    private MaintenanceDataResetService maintenanceDataResetService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DocumentStorage documentStorage;

    @BeforeEach
    void setUp() {
        inMemoryStorage().clear();
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
                properties,
                maintenanceDataResetService);

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
                                "TempPassword123!")),
                maintenanceDataResetService);

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
                                "TempPassword123!")),
                maintenanceDataResetService);

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
                                "TempPassword123!")),
                maintenanceDataResetService);

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

    @Test
    void shouldNotPruneToInternalCoreWhenBootstrapAdminIsDisabled() {
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
                                false,
                                "vanderson.verza@gmail.com",
                                "Vanderson Verza",
                                true,
                                true,
                                "PermanentPassword123!",
                                "TempPassword123!")),
                maintenanceDataResetService);

        service.seedIfEmpty();

        Assertions.assertThat(organizationLookup.findById("internal-core")).isPresent();
        Assertions.assertThat(organizationLookup.findById("tenant-a")).isPresent();
        Assertions.assertThat(organizationLookup.findById("tenant-b")).isPresent();
        Assertions.assertThat(userRepository.findByEmailIgnoreCase("admin.a@tenant.com")).isPresent();
        Assertions.assertThat(userRepository.findByEmailIgnoreCase("vanderson.verza@gmail.com")).isNotPresent();
    }

    @Test
    void shouldClearRuntimeModuleDataAndKeepBaselineCatalogsDuringReset() {
        jdbcTemplate.update(
                """
                INSERT INTO project (
                    id, tenant_id, code, name, description, framework_type, template_id, template_version,
                    lead_organization_id, customer_organization_id, status, visibility_scope,
                    planned_start_date, planned_end_date, actual_start_date, actual_end_date,
                    created_by_user_id, created_at, updated_at, version
                ) VALUES (
                    'PRJ-RESET-001', (SELECT tenant_id FROM organization WHERE id = 'internal-core'),
                    'RESET-001', 'Reset Fixture', NULL, 'CUSTOM', 'TMP-CUSTOM-V1', 1,
                    'internal-core', NULL, 'ACTIVE', 'ALL_PROJECT_PARTICIPANTS',
                    NULL, NULL, NULL, NULL, 'USR-RESET-001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
                )
                """);
        jdbcTemplate.update(
                """
                INSERT INTO document (
                    id, tenant_id, original_filename, safe_filename, content_type, extension, size_bytes,
                    checksum_sha256, storage_provider, storage_key, status, uploaded_by_user_id,
                    uploaded_by_organization_id, upload_expires_at, created_at, updated_at, deleted_at
                ) VALUES (
                    'DOC-RESET-001', (SELECT tenant_id FROM organization WHERE id = 'internal-core'),
                    'fixture.txt', 'fixture.txt', 'text/plain', 'txt', 1,
                    'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa',
                    'S3', 'reset/fixture.txt', 'AVAILABLE', 'USR-RESET-001',
                    'internal-core', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL
                )
                """);
        inMemoryStorage().putObject("reset/fixture.txt", "text/plain", new byte[] {1}, java.util.Map.of());
        inMemoryStorage().putObject("tenant/orphan.txt", "text/plain", new byte[] {1}, java.util.Map.of());
        jdbcTemplate.update(
                """
                INSERT INTO project_framework (
                    id, code, display_name, description, ui_layout, active, created_at, updated_at
                ) VALUES (
                    'PFR-FAKE', 'FAKE', 'Fake Framework', 'Temporary test framework.', 'HYBRID',
                    TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                )
                """);
        jdbcTemplate.update(
                """
                INSERT INTO project_structure_template (
                    id, name, framework_type, version, active, created_at, owner_organization_id
                ) VALUES (
                    'PST-FAKE-V1', 'Fake Structure v1', 'FAKE', 1, TRUE, CURRENT_TIMESTAMP, 'internal-core'
                )
                """);
        jdbcTemplate.update(
                """
                INSERT INTO project_structure_level_template (
                    id, structure_template_id, sequence_no, name, code, allows_children,
                    allows_milestones, allows_deliverables
                ) VALUES (
                    'PSLT-FAKE-001', 'PST-FAKE-V1', 1, 'Fake Level', 'FAKE_LEVEL',
                    FALSE, TRUE, TRUE
                )
                """);
        jdbcTemplate.update(
                """
                INSERT INTO project_template (
                    id, name, framework_type, version, status, is_default, created_at,
                    structure_template_id, owner_organization_id
                ) VALUES (
                    'TMP-FAKE-V1', 'Fake Project Template v1', 'FAKE', 1, 'ACTIVE',
                    FALSE, CURRENT_TIMESTAMP, 'PST-FAKE-V1', 'internal-core'
                )
                """);

        resetWithProperties(new BootstrapProperties(
                false,
                new BootstrapProperties.InternalAdminProperties(
                        true,
                        "vanderson.verza@gmail.com",
                        "Vanderson Verza",
                        true,
                        false,
                        "PermanentPassword123!",
                        "TempPassword123!")));

        Assertions.assertThat(countRows("project")).isZero();
        Assertions.assertThat(countRows("document")).isZero();
        Assertions.assertThat(countRows("project_template")).isGreaterThan(0);
        Assertions.assertThat(countRows("project_framework")).isGreaterThan(0);
        Assertions.assertThat(countRowsWhere("project_template", "id = 'TMP-FAKE-V1'")).isZero();
        Assertions.assertThat(countRowsWhere("project_structure_template", "id = 'PST-FAKE-V1'")).isZero();
        Assertions.assertThat(countRowsWhere("project_structure_level_template", "id = 'PSLT-FAKE-001'")).isZero();
        Assertions.assertThat(countRowsWhere("project_framework", "code = 'FAKE'")).isZero();
        Assertions.assertThat(countRowsWhere("project_template", "id IN ('TMP-APQP-V1', 'TMP-VDA-MLA-V1', 'TMP-CUSTOM-V1')"))
                .isEqualTo(3);
        Assertions.assertThat(countRowsWhere("project_structure_template", "id IN ('PST-APQP-V1', 'PST-VDA-MLA-V1', 'PST-CUSTOM-V1')"))
                .isEqualTo(3);
        Assertions.assertThat(countRowsWhere("project_framework", "code IN ('APQP', 'VDA_MLA', 'CUSTOM')"))
                .isEqualTo(3);
        Assertions.assertThat(documentStorage.headObject("reset/fixture.txt").exists()).isFalse();
        Assertions.assertThat(documentStorage.headObject("tenant/orphan.txt").exists()).isFalse();
        Assertions.assertThat(organizationLookup.findAll())
                .extracting(OrganizationLookup.OrganizationView::id)
                .containsExactly("internal-core");
        Assertions.assertThat(userRepository.findAll())
                .extracting(ManagedUser::email)
                .containsExactly("vanderson.verza@gmail.com");
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
                properties,
                maintenanceDataResetService);
        service.reset();
    }

    private int countRows(String tableName) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Integer.class);
    }

    private int countRowsWhere(String tableName, String whereClause) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName + " WHERE " + whereClause, Integer.class);
    }

    private InMemoryDocumentStorageStub inMemoryStorage() {
        return (InMemoryDocumentStorageStub) documentStorage;
    }

    @TestConfiguration
    static class DocumentStorageTestConfig {

        @Bean
        @Primary
        InMemoryDocumentStorageStub documentStorage() {
            return new InMemoryDocumentStorageStub();
        }
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
