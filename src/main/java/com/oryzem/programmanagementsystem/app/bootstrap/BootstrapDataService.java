package com.oryzem.programmanagementsystem.app.bootstrap;

import com.oryzem.programmanagementsystem.platform.audit.AuditTrailService;
import com.oryzem.programmanagementsystem.platform.authorization.Role;
import com.oryzem.programmanagementsystem.platform.authorization.TenantType;
import com.oryzem.programmanagementsystem.modules.projectmanagement.PortfolioResetPort;
import com.oryzem.programmanagementsystem.modules.operations.OperationRecord;
import com.oryzem.programmanagementsystem.modules.operations.OperationRepository;
import com.oryzem.programmanagementsystem.modules.operations.OperationStatus;
import com.oryzem.programmanagementsystem.platform.tenant.OrganizationBootstrapPort;
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
    private final OperationRepository operationRepository;
    private final AuditTrailService auditTrailService;
    private final PortfolioResetPort portfolioResetPort;
    private final OrganizationBootstrapPort organizationBootstrapPort;
    private final UserIdentityGateway userIdentityGateway;
    private final BootstrapProperties bootstrapProperties;

    public BootstrapDataService(
            UserRepository userRepository,
            OperationRepository operationRepository,
            AuditTrailService auditTrailService,
            PortfolioResetPort portfolioResetPort,
            OrganizationBootstrapPort organizationBootstrapPort,
            UserIdentityGateway userIdentityGateway,
            BootstrapProperties bootstrapProperties) {
        this.userRepository = userRepository;
        this.operationRepository = operationRepository;
        this.auditTrailService = auditTrailService;
        this.portfolioResetPort = portfolioResetPort;
        this.organizationBootstrapPort = organizationBootstrapPort;
        this.userIdentityGateway = userIdentityGateway;
        this.bootstrapProperties = bootstrapProperties;
    }

    @PostConstruct
    @Transactional
    public void seedIfEmpty() {
        ensureInternalCoreOrganization();

        if (bootstrapProperties.seedData()) {
            if (userRepository.findAll().isEmpty() && operationRepository.findAll().isEmpty()) {
                seedUsers();
                seedOperations();
            }
        }

        ensureBootstrapInternalAdmin();
    }

    @Transactional
    public void reset() {
        auditTrailService.clear();
        operationRepository.deleteAll();
        userRepository.deleteAll();
        portfolioResetPort.clearAll();
        ensureInternalCoreOrganization();
        if (bootstrapProperties.seedData()) {
            seedUsers();
            seedOperations();
        }
        ensureBootstrapInternalAdmin();
    }

    private void seedUsers() {
        Instant baseTime = Instant.parse("2026-03-07T12:00:00Z");
        seedOrganizations();
        userRepository.save(user("USR-ADMIN-001", "Platform Admin", "admin@oryzem.com", Role.ADMIN, "internal-core", TenantType.INTERNAL, UserStatus.ACTIVE, baseTime));
        userRepository.save(user("USR-ADMIN-002", "Security Admin", "security.admin@oryzem.com", Role.ADMIN, "internal-core", TenantType.INTERNAL, UserStatus.ACTIVE, baseTime.plusSeconds(60)));
        userRepository.save(user("USR-INT-SUP-001", "Support Analyst", "support@oryzem.com", Role.SUPPORT, "internal-core", TenantType.INTERNAL, UserStatus.ACTIVE, baseTime.plusSeconds(120)));
        userRepository.save(user("USR-EXT-A-ADM-001", "Tenant A Admin", "admin.a@tenant.com", Role.ADMIN, "tenant-a", TenantType.EXTERNAL, UserStatus.ACTIVE, baseTime.plusSeconds(180)));
        userRepository.save(user("USR-EXT-A-MGR-001", "Tenant A Manager", "manager.a@tenant.com", Role.MANAGER, "tenant-a", TenantType.EXTERNAL, UserStatus.ACTIVE, baseTime.plusSeconds(240)));
        userRepository.save(user("USR-EXT-A-MEM-001", "Tenant A Member", "member.a@tenant.com", Role.MEMBER, "tenant-a", TenantType.EXTERNAL, UserStatus.ACTIVE, baseTime.plusSeconds(300)));
        userRepository.save(user("USR-EXT-B-ADM-001", "Tenant B Admin", "admin.b@tenant.com", Role.ADMIN, "tenant-b", TenantType.EXTERNAL, UserStatus.ACTIVE, baseTime.plusSeconds(360)));
        userRepository.save(user("USR-EXT-B-MGR-001", "Tenant B Manager", "manager.b@tenant.com", Role.MANAGER, "tenant-b", TenantType.EXTERNAL, UserStatus.ACTIVE, baseTime.plusSeconds(420)));
        userRepository.save(user("USR-EXT-B-MEM-001", "Tenant B Member", "member.b@tenant.com", Role.MEMBER, "tenant-b", TenantType.EXTERNAL, UserStatus.ACTIVE, baseTime.plusSeconds(480)));
        userRepository.save(user("USR-EXT-B-AUD-001", "Tenant B Auditor", "auditor.b@tenant.com", Role.AUDITOR, "tenant-b", TenantType.EXTERNAL, UserStatus.ACTIVE, baseTime.plusSeconds(540)));
    }

    private void seedOperations() {
        Instant baseTime = Instant.parse("2026-03-07T12:30:00Z");
        operationRepository.save(operation("OP-TA-001", "APQP Kickoff", "Initial kickoff for supplier A", "tenant-a", TenantType.EXTERNAL, "manager-123", OperationStatus.DRAFT, baseTime));
        operationRepository.save(operation("OP-TA-002", "PPAP Package", "Prepare PPAP package for approval", "tenant-a", TenantType.EXTERNAL, "member-123", OperationStatus.SUBMITTED, baseTime.plusSeconds(120)));
        operationRepository.save(operation("OP-TA-003", "Run @ Rate", "Capacity validation for tenant A", "tenant-a", TenantType.EXTERNAL, "member-123", OperationStatus.DRAFT, baseTime.plusSeconds(180)));
        operationRepository.save(operation("OP-TB-001", "Line Trial", "Production line trial for tenant B", "tenant-b", TenantType.EXTERNAL, "manager-b", OperationStatus.APPROVED, baseTime.plusSeconds(240)));
        operationRepository.save(operation("OP-TB-002", "Corrective Action", "Corrective action follow-up", "tenant-b", TenantType.EXTERNAL, "member-b", OperationStatus.REJECTED, baseTime.plusSeconds(360)));
    }

    private void seedOrganizations() {
        organizationBootstrapPort.ensureSeeded(
                INTERNAL_CORE_ORGANIZATION_ID,
                BOOTSTRAP_ACTOR,
                "Oryzem Internal Core",
                "CORE-INT",
                TenantType.INTERNAL,
                null,
                true);
        organizationBootstrapPort.ensureSeeded(
                "tenant-a",
                BOOTSTRAP_ACTOR,
                "Tenant A",
                "TENANT-A",
                TenantType.EXTERNAL,
                null,
                true);
        organizationBootstrapPort.ensureSeeded(
                "tenant-b",
                BOOTSTRAP_ACTOR,
                "Tenant B",
                "TENANT-B",
                TenantType.EXTERNAL,
                null,
                true);
    }

    private void ensureInternalCoreOrganization() {
        organizationBootstrapPort.ensureSeeded(
                INTERNAL_CORE_ORGANIZATION_ID,
                BOOTSTRAP_ACTOR,
                "Oryzem Internal Core",
                "CORE-INT",
                TenantType.INTERNAL,
                null,
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
        pruneOtherInternalUsersIfRequested(internalAdmin, saved.email());
        log.info(
                "Ensured INTERNAL ADMIN break-glass user for organization '{}': {} with roles {}",
                INTERNAL_CORE_ORGANIZATION_ID,
                saved.email(),
                INTERNAL_ADMIN_BREAK_GLASS_ROLES);
    }

    private ManagedUser alignBootstrapInternalAdmin(ManagedUser existing, String displayName) {
        if (existing.tenantType() != TenantType.INTERNAL
                || !INTERNAL_CORE_ORGANIZATION_ID.equals(existing.tenantId())
                || existing.role() != Role.ADMIN) {
            throw new IllegalStateException(
                    "Bootstrap INTERNAL ADMIN email already belongs to another user and cannot be reused safely: "
                            + existing.email());
        }

        ManagedUser aligned = existing;
        if (!displayName.equals(existing.displayName())) {
            aligned = aligned.withUpdatedDetails(
                    displayName,
                    existing.email(),
                    existing.role(),
                    existing.tenantId(),
                    existing.tenantType());
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
                Role.ADMIN,
                INTERNAL_CORE_ORGANIZATION_ID,
                TenantType.INTERNAL,
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
                .filter(user -> user.tenantType() == TenantType.INTERNAL)
                .filter(user -> !equalsIgnoreCase(user.email(), preservedEmail))
                .toList();

        for (ManagedUser user : usersToDelete) {
            try {
                userIdentityGateway.deleteUser(user);
            } catch (RuntimeException exception) {
                log.warn(
                        "Unable to delete legacy INTERNAL user '{}' from Cognito during break-glass prune. "
                                + "Proceeding with local cleanup only. reason={}",
                        user.email(),
                        exception.getMessage());
            }
            userRepository.deleteById(user.id());
            log.info(
                    "Pruned legacy INTERNAL user during break-glass bootstrap. organization='{}', email={}",
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
            Role role,
            String tenantId,
            TenantType tenantType,
            UserStatus status,
            Instant createdAt) {
        return new ManagedUser(
                id,
                email.toLowerCase(java.util.Locale.ROOT),
                null,
                displayName,
                email,
                role,
                tenantId,
                tenantType,
                status,
                createdAt,
                null,
                null);
    }

    private OperationRecord operation(
            String id,
            String title,
            String description,
            String tenantId,
            TenantType tenantType,
            String createdBy,
            OperationStatus status,
            Instant createdAt) {
        Instant submittedAt = status == OperationStatus.SUBMITTED ? createdAt.plusSeconds(30) : null;
        Instant approvedAt = status == OperationStatus.APPROVED ? createdAt.plusSeconds(60) : null;
        Instant rejectedAt = status == OperationStatus.REJECTED ? createdAt.plusSeconds(60) : null;
        return new OperationRecord(
                id,
                title,
                description,
                tenantId,
                tenantType,
                createdBy,
                status,
                createdAt,
                createdAt,
                submittedAt,
                approvedAt,
                rejectedAt,
                null,
                null);
    }
}


