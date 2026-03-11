package com.oryzem.programmanagementsystem.bootstrap;

import com.oryzem.programmanagementsystem.audit.AuditTrailService;
import com.oryzem.programmanagementsystem.authorization.Role;
import com.oryzem.programmanagementsystem.authorization.TenantType;
import com.oryzem.programmanagementsystem.operations.OperationRecord;
import com.oryzem.programmanagementsystem.operations.OperationRepository;
import com.oryzem.programmanagementsystem.operations.OperationStatus;
import com.oryzem.programmanagementsystem.users.ManagedUser;
import com.oryzem.programmanagementsystem.users.UserRepository;
import com.oryzem.programmanagementsystem.users.UserStatus;
import jakarta.annotation.PostConstruct;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BootstrapDataService {

    private final UserRepository userRepository;
    private final OperationRepository operationRepository;
    private final AuditTrailService auditTrailService;
    private final boolean seedData;

    public BootstrapDataService(
            UserRepository userRepository,
            OperationRepository operationRepository,
            AuditTrailService auditTrailService,
            @Value("${app.bootstrap.seed-data:true}") boolean seedData) {
        this.userRepository = userRepository;
        this.operationRepository = operationRepository;
        this.auditTrailService = auditTrailService;
        this.seedData = seedData;
    }

    @PostConstruct
    @Transactional
    public void seedIfEmpty() {
        if (!seedData) {
            return;
        }

        if (!userRepository.findAll().isEmpty() || !operationRepository.findAll().isEmpty()) {
            return;
        }

        seedUsers();
        seedOperations();
    }

    @Transactional
    public void reset() {
        auditTrailService.clear();
        operationRepository.deleteAll();
        userRepository.deleteAll();
        if (seedData) {
            seedUsers();
            seedOperations();
        }
    }

    private void seedUsers() {
        Instant baseTime = Instant.parse("2026-03-07T12:00:00Z");
        userRepository.save(user("USR-ADMIN-001", "Platform Admin", "admin@oryzem.com", Role.ADMIN, "internal-core", TenantType.INTERNAL, UserStatus.ACTIVE, baseTime));
        userRepository.save(user("USR-ADMIN-002", "Security Admin", "security.admin@oryzem.com", Role.ADMIN, "internal-core", TenantType.INTERNAL, UserStatus.ACTIVE, baseTime.plusSeconds(60)));
        userRepository.save(user("USR-INT-SUP-001", "Support Analyst", "support@oryzem.com", Role.SUPPORT, "internal-core", TenantType.INTERNAL, UserStatus.ACTIVE, baseTime.plusSeconds(120)));
        userRepository.save(user("USR-EXT-A-MGR-001", "Tenant A Manager", "manager.a@tenant.com", Role.MANAGER, "tenant-a", TenantType.EXTERNAL, UserStatus.ACTIVE, baseTime.plusSeconds(180)));
        userRepository.save(user("USR-EXT-A-MEM-001", "Tenant A Member", "member.a@tenant.com", Role.MEMBER, "tenant-a", TenantType.EXTERNAL, UserStatus.ACTIVE, baseTime.plusSeconds(240)));
        userRepository.save(user("USR-EXT-B-MGR-001", "Tenant B Manager", "manager.b@tenant.com", Role.MANAGER, "tenant-b", TenantType.EXTERNAL, UserStatus.ACTIVE, baseTime.plusSeconds(300)));
        userRepository.save(user("USR-EXT-B-MEM-001", "Tenant B Member", "member.b@tenant.com", Role.MEMBER, "tenant-b", TenantType.EXTERNAL, UserStatus.ACTIVE, baseTime.plusSeconds(360)));
        userRepository.save(user("USR-EXT-B-AUD-001", "Tenant B Auditor", "auditor.b@tenant.com", Role.AUDITOR, "tenant-b", TenantType.EXTERNAL, UserStatus.ACTIVE, baseTime.plusSeconds(420)));
    }

    private void seedOperations() {
        Instant baseTime = Instant.parse("2026-03-07T12:30:00Z");
        operationRepository.save(operation("OP-TA-001", "APQP Kickoff", "Initial kickoff for supplier A", "tenant-a", TenantType.EXTERNAL, "manager-123", OperationStatus.DRAFT, baseTime));
        operationRepository.save(operation("OP-TA-002", "PPAP Package", "Prepare PPAP package for approval", "tenant-a", TenantType.EXTERNAL, "member-123", OperationStatus.SUBMITTED, baseTime.plusSeconds(120)));
        operationRepository.save(operation("OP-TA-003", "Run @ Rate", "Capacity validation for tenant A", "tenant-a", TenantType.EXTERNAL, "member-123", OperationStatus.DRAFT, baseTime.plusSeconds(180)));
        operationRepository.save(operation("OP-TB-001", "Line Trial", "Production line trial for tenant B", "tenant-b", TenantType.EXTERNAL, "manager-b", OperationStatus.APPROVED, baseTime.plusSeconds(240)));
        operationRepository.save(operation("OP-TB-002", "Corrective Action", "Corrective action follow-up", "tenant-b", TenantType.EXTERNAL, "member-b", OperationStatus.REJECTED, baseTime.plusSeconds(360)));
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
        return new ManagedUser(id, displayName, email, role, tenantId, tenantType, status, createdAt, null, null);
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
