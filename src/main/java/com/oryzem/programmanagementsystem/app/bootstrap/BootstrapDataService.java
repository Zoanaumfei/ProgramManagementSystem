package com.oryzem.programmanagementsystem.app.bootstrap;

import com.oryzem.programmanagementsystem.platform.audit.AuditTrailService;
import com.oryzem.programmanagementsystem.platform.authorization.Role;
import com.oryzem.programmanagementsystem.platform.authorization.TenantType;
import com.oryzem.programmanagementsystem.platform.access.AccessContextResetService;
import com.oryzem.programmanagementsystem.platform.access.AccessContextService;
import com.oryzem.programmanagementsystem.platform.tenant.OrganizationBootstrapPort;
import com.oryzem.programmanagementsystem.platform.tenant.OrganizationResetPort;
import com.oryzem.programmanagementsystem.platform.users.domain.ManagedUser;
import com.oryzem.programmanagementsystem.platform.users.domain.UserIdentityGateway;
import com.oryzem.programmanagementsystem.platform.users.domain.UserRepository;
import com.oryzem.programmanagementsystem.platform.users.domain.UserStatus;
import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BootstrapDataService {

    private static final Logger log = LoggerFactory.getLogger(BootstrapDataService.class);
    private static final String INTERNAL_CORE_ORGANIZATION_ID = "internal-core";
    private static final String BOOTSTRAP_ACTOR = "bootstrap";
    private static final Set<Role> INTERNAL_ADMIN_BREAK_GLASS_ROLES =
            EnumSet.of(Role.ADMIN, Role.SUPPORT, Role.AUDITOR);

    private final UserRepository userRepository;
    private final AuditTrailService auditTrailService;
    private final AccessContextService accessContextService;
    private final AccessContextResetService accessContextResetService;
    private final OrganizationBootstrapPort organizationBootstrapPort;
    private final OrganizationResetPort organizationResetPort;
    private final UserIdentityGateway userIdentityGateway;
    private final BootstrapProperties bootstrapProperties;
    private final MaintenanceDataResetService maintenanceDataResetService;

    public BootstrapDataService(
            UserRepository userRepository,
            AuditTrailService auditTrailService,
            AccessContextService accessContextService,
            AccessContextResetService accessContextResetService,
            OrganizationBootstrapPort organizationBootstrapPort,
            OrganizationResetPort organizationResetPort,
            UserIdentityGateway userIdentityGateway,
            BootstrapProperties bootstrapProperties,
            MaintenanceDataResetService maintenanceDataResetService) {
        this.userRepository = userRepository;
        this.auditTrailService = auditTrailService;
        this.accessContextService = accessContextService;
        this.accessContextResetService = accessContextResetService;
        this.organizationBootstrapPort = organizationBootstrapPort;
        this.organizationResetPort = organizationResetPort;
        this.userIdentityGateway = userIdentityGateway;
        this.bootstrapProperties = bootstrapProperties;
        this.maintenanceDataResetService = maintenanceDataResetService;
    }

    @PostConstruct
    @Transactional
    public void seedIfEmpty() {
        BootstrapProperties.InternalAdminProperties internalAdmin = bootstrapProperties.internalAdmin();
        if (internalAdmin != null && internalAdmin.enabled() && internalAdmin.pruneToInternalCore()) {
            resetToInternalCoreSkeleton();
        } else {
            ensureInternalCoreOrganization();

            if (bootstrapProperties.seedData() && userRepository.findAll().isEmpty()) {
                seedUsers();
            }
        }

        ensureBootstrapInternalAdmin();
    }

    @Transactional
    public void reset() {
        maintenanceDataResetService.clearRuntimeData();
        auditTrailService.clear();
        accessContextResetService.clearMemberships();
        userRepository.deleteAll();
        organizationResetPort.clearOrganizations();
        accessContextResetService.clearTenantsAndMarkets();
        ensureInternalCoreOrganization();
        if (bootstrapProperties.seedData()) {
            seedUsers();
        }
        ensureBootstrapInternalAdmin();
    }

    private void resetToInternalCoreSkeleton() {
        maintenanceDataResetService.clearRuntimeData();
        auditTrailService.clear();
        accessContextResetService.clearMemberships();
        userRepository.deleteAll();
        organizationResetPort.clearOrganizations();
        accessContextResetService.clearTenantsAndMarkets();
        ensureInternalCoreOrganization();
    }

    private void seedUsers() {
        Instant baseTime = Instant.parse("2026-03-07T12:00:00Z");
        seedOrganizations();
        seedUserWithMembership(user("USR-ADMIN-001", "Platform Admin", "admin@oryzem.com", UserStatus.ACTIVE, baseTime), "internal-core", Set.of(Role.ADMIN));
        seedUserWithMembership(user("USR-ADMIN-002", "Security Admin", "security.admin@oryzem.com", UserStatus.ACTIVE, baseTime.plusSeconds(60)), "internal-core", Set.of(Role.ADMIN));
        seedUserWithMembership(user("USR-INT-SUP-001", "Support Analyst", "support@oryzem.com", UserStatus.ACTIVE, baseTime.plusSeconds(120)), "internal-core", Set.of(Role.SUPPORT));
        seedUserWithMembership(user("USR-EXT-A-ADM-001", "Tenant A Admin", "admin.a@tenant.com", UserStatus.ACTIVE, baseTime.plusSeconds(180)), "tenant-a", Set.of(Role.ADMIN));
        seedUserWithMembership(user("USR-EXT-A-SUP-001", "Tenant A Support", "support.a@tenant.com", UserStatus.ACTIVE, baseTime.plusSeconds(240)), "tenant-a", Set.of(Role.SUPPORT));
        seedUserWithMembership(user("USR-EXT-A-MGR-001", "Tenant A Manager", "manager.a@tenant.com", UserStatus.ACTIVE, baseTime.plusSeconds(300)), "tenant-a", Set.of(Role.MANAGER));
        seedUserWithMembership(user("USR-EXT-A-MEM-001", "Tenant A Member", "member.a@tenant.com", UserStatus.ACTIVE, baseTime.plusSeconds(360)), "tenant-a", Set.of(Role.MEMBER));
        seedUserWithMembership(user("USR-EXT-B-ADM-001", "Tenant B Admin", "admin.b@tenant.com", UserStatus.ACTIVE, baseTime.plusSeconds(360)), "tenant-b", Set.of(Role.ADMIN));
        seedUserWithMembership(user("USR-EXT-B-MGR-001", "Tenant B Manager", "manager.b@tenant.com", UserStatus.ACTIVE, baseTime.plusSeconds(420)), "tenant-b", Set.of(Role.MANAGER));
        seedUserWithMembership(user("USR-EXT-B-MEM-001", "Tenant B Member", "member.b@tenant.com", UserStatus.ACTIVE, baseTime.plusSeconds(480)), "tenant-b", Set.of(Role.MEMBER));
        seedUserWithMembership(user("USR-EXT-B-AUD-001", "Tenant B Auditor", "auditor.b@tenant.com", UserStatus.ACTIVE, baseTime.plusSeconds(540)), "tenant-b", Set.of(Role.AUDITOR));
    }

    private void seedOrganizations() {
        organizationBootstrapPort.ensureSeeded(
                INTERNAL_CORE_ORGANIZATION_ID,
                BOOTSTRAP_ACTOR,
                "Oryzem Internal Core",
                null,
                TenantType.INTERNAL,
                true);
        organizationBootstrapPort.ensureSeeded(
                "tenant-a",
                BOOTSTRAP_ACTOR,
                "Tenant A",
                "12345678000195",
                TenantType.EXTERNAL,
                true);
        organizationBootstrapPort.ensureSeeded(
                "tenant-b",
                BOOTSTRAP_ACTOR,
                "Tenant B",
                "98765432000198",
                TenantType.EXTERNAL,
                true);
    }

    private void ensureInternalCoreOrganization() {
        organizationBootstrapPort.ensureSeeded(
                INTERNAL_CORE_ORGANIZATION_ID,
                BOOTSTRAP_ACTOR,
                "Oryzem Internal Core",
                null,
                TenantType.INTERNAL,
                true);
    }

    private void ensureBootstrapInternalAdmin() {
        BootstrapProperties.InternalAdminProperties internalAdmin = bootstrapProperties.internalAdmin();
        if (internalAdmin == null || !internalAdmin.enabled()) {
            return;
        }

        String normalizedEmail = normalizeRequiredEmail(internalAdmin.email());
        String normalizedDisplayName = normalizeRequiredDisplayName(internalAdmin.displayName());
        ManagedUser bootstrapUser = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .map(existing -> alignBootstrapInternalAdmin(existing, normalizedDisplayName))
                .orElseGet(() -> newBootstrapInternalAdmin(normalizedEmail, normalizedDisplayName));

        userIdentityGateway.ensureBootstrapUser(
                bootstrapUser,
                INTERNAL_ADMIN_BREAK_GLASS_ROLES,
                blankToNull(internalAdmin.password()),
                blankToNull(internalAdmin.temporaryPassword()));
        ManagedUser saved = userRepository.save(bootstrapUser);
        accessContextService.upsertDefaultMembership(
                saved.id(),
                accessContextService.resolveTenantBoundaryId(INTERNAL_CORE_ORGANIZATION_ID),
                INTERNAL_CORE_ORGANIZATION_ID,
                null,
                saved.status(),
                INTERNAL_ADMIN_BREAK_GLASS_ROLES,
                saved.createdAt());
        pruneOtherInternalUsersIfRequested(internalAdmin, saved.email());
        log.info(
                "Ensured INTERNAL ADMIN break-glass user for organization '{}': {} with roles {}",
                INTERNAL_CORE_ORGANIZATION_ID,
                saved.email(),
                INTERNAL_ADMIN_BREAK_GLASS_ROLES);
    }

    private ManagedUser alignBootstrapInternalAdmin(ManagedUser existing, String displayName) {
        var existingContext = accessContextService.resolveActiveContext(existing, null)
                .orElseThrow(() -> new IllegalStateException(
                        "Bootstrap INTERNAL ADMIN email already belongs to a user without an active membership: "
                                + existing.email()));
        if (existingContext.tenantType() != TenantType.INTERNAL
                || !INTERNAL_CORE_ORGANIZATION_ID.equals(existingContext.activeOrganizationId())
                || accessContextService.resolvePrimaryRole(existing).orElse(null) != Role.ADMIN) {
            throw new IllegalStateException(
                    "Bootstrap INTERNAL ADMIN email already belongs to another user and cannot be reused safely: "
                            + existing.email());
        }

        ManagedUser aligned = existing;
        if (!displayName.equals(existing.displayName())) {
            aligned = aligned.withUpdatedDetails(displayName, existing.email());
        }
        if (aligned.status() != UserStatus.ACTIVE) {
            aligned = aligned.withStatus(UserStatus.ACTIVE);
        }
        return aligned;
    }

    private ManagedUser newBootstrapInternalAdmin(String normalizedEmail, String normalizedDisplayName) {
        return new ManagedUser(
                "USR-BOOTSTRAP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT),
                normalizedEmail,
                null,
                normalizedDisplayName,
                normalizedEmail,
                UserStatus.ACTIVE,
                Instant.now(),
                null,
                null);
    }

    private void pruneOtherInternalUsersIfRequested(
            BootstrapProperties.InternalAdminProperties internalAdmin,
            String preservedEmail) {
        if (internalAdmin == null || !internalAdmin.pruneOtherInternalUsers()) {
            return;
        }

        List<ManagedUser> usersToDelete = userRepository.findByTenantId(INTERNAL_CORE_ORGANIZATION_ID).stream()
                .filter(user -> accessContextService.resolvePrimaryTenantType(user).orElse(null) == TenantType.INTERNAL)
                .filter(user -> !equalsIgnoreCase(user.email(), preservedEmail))
                .toList();

        for (ManagedUser user : usersToDelete) {
            try {
                userIdentityGateway.deleteUser(user);
            } catch (RuntimeException exception) {
                log.warn(
                        "Unable to delete stale INTERNAL user '{}' from Cognito during break-glass prune. "
                                + "Proceeding with local cleanup only. reason={}",
                        user.email(),
                        exception.getMessage());
            }
            userRepository.deleteById(user.id());
            log.info(
                    "Pruned stale INTERNAL user during break-glass bootstrap. organization='{}', email={}",
                    INTERNAL_CORE_ORGANIZATION_ID,
                    user.email());
        }
    }

    private String normalizeRequiredEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalStateException(
                    "Bootstrap INTERNAL ADMIN requires app.bootstrap.internal-admin.email when enabled.");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeRequiredDisplayName(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalStateException(
                    "Bootstrap INTERNAL ADMIN requires app.bootstrap.internal-admin.display-name when enabled.");
        }
        return displayName.trim();
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private boolean equalsIgnoreCase(String left, String right) {
        if (left == null || right == null) {
            return left == null && right == null;
        }
        return left.equalsIgnoreCase(right);
    }

    private ManagedUser user(
            String id,
            String displayName,
            String email,
            UserStatus status,
            Instant createdAt) {
        return new ManagedUser(
                id,
                email.toLowerCase(java.util.Locale.ROOT),
                null,
                displayName,
                email,
                status,
                createdAt,
                null,
                null);
    }

    private void seedUserWithMembership(ManagedUser user, String organizationId, Set<Role> roles) {
        ManagedUser saved = userRepository.save(user);
        accessContextService.upsertDefaultMembership(
                saved.id(),
                accessContextService.resolveTenantBoundaryId(organizationId),
                organizationId,
                null,
                saved.status(),
                roles,
                saved.createdAt());
    }
}
