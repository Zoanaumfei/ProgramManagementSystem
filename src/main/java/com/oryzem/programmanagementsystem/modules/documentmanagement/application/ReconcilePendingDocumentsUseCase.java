package com.oryzem.programmanagementsystem.modules.documentmanagement.application;

import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentStatus;
import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentStorage;
import com.oryzem.programmanagementsystem.modules.documentmanagement.infrastructure.DocumentBindingEntity;
import com.oryzem.programmanagementsystem.modules.documentmanagement.infrastructure.DocumentEntity;
import com.oryzem.programmanagementsystem.modules.documentmanagement.infrastructure.SpringDataDocumentBindingJpaRepository;
import com.oryzem.programmanagementsystem.modules.documentmanagement.infrastructure.SpringDataDocumentJpaRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReconcilePendingDocumentsUseCase {

    private final SpringDataDocumentJpaRepository documentRepository;
    private final SpringDataDocumentBindingJpaRepository bindingRepository;
    private final DocumentStorage documentStorage;
    private final DocumentAuditService auditService;

    public ReconcilePendingDocumentsUseCase(
            SpringDataDocumentJpaRepository documentRepository,
            SpringDataDocumentBindingJpaRepository bindingRepository,
            DocumentStorage documentStorage,
            DocumentAuditService auditService) {
        this.documentRepository = documentRepository;
        this.bindingRepository = bindingRepository;
        this.documentStorage = documentStorage;
        this.auditService = auditService;
    }

    @Transactional
    public ReconciliationResult execute() {
        Instant now = Instant.now();
        int expiredPendingMarkedFailed = 0;
        int failedRecovered = 0;
        int missingActiveObjects = 0;
        int orphanObjectsDetected = 0;

        Map<String, DocumentBindingEntity> bindingsByDocumentId = new HashMap<>();
        bindingRepository.findAll().forEach(binding -> bindingsByDocumentId.put(binding.getDocumentId(), binding));

        for (DocumentEntity pending : documentRepository.findAllByStatusAndUploadExpiresAtBefore(DocumentStatus.PENDING_UPLOAD, now)) {
            if (!documentStorage.headObject(pending.getStorageKey()).exists()) {
                pending.markFailed(now);
                documentRepository.save(pending);
                expiredPendingMarkedFailed++;
                DocumentBindingEntity binding = bindingsByDocumentId.get(pending.getId());
                auditService.recordSystem(
                        "DOCUMENT_INCONSISTENCY_DETECTED",
                        pending.getTenantId(),
                        pending.getId(),
                        binding != null ? binding.getContextType() : null,
                        binding != null ? binding.getContextId() : null,
                        "PENDING_EXPIRED",
                        Map.of("storageKey", pending.getStorageKey()));
            }
        }

        for (DocumentEntity failed : documentRepository.findAllByStatus(DocumentStatus.FAILED)) {
            if (documentStorage.headObject(failed.getStorageKey()).exists()) {
                failed.markActive(now);
                documentRepository.save(failed);
                failedRecovered++;
                DocumentBindingEntity binding = bindingsByDocumentId.get(failed.getId());
                auditService.recordSystem(
                        "DOCUMENT_UPLOAD_FINALIZED",
                        failed.getTenantId(),
                        failed.getId(),
                        binding != null ? binding.getContextType() : null,
                        binding != null ? binding.getContextId() : null,
                        "RECOVERED",
                        Map.of("storageKey", failed.getStorageKey()));
            }
        }

        for (DocumentEntity active : documentRepository.findAllByStatus(DocumentStatus.ACTIVE)) {
            if (!documentStorage.headObject(active.getStorageKey()).exists()) {
                missingActiveObjects++;
                DocumentBindingEntity binding = bindingsByDocumentId.get(active.getId());
                auditService.recordSystem(
                        "DOCUMENT_INCONSISTENCY_DETECTED",
                        active.getTenantId(),
                        active.getId(),
                        binding != null ? binding.getContextType() : null,
                        binding != null ? binding.getContextId() : null,
                        "ACTIVE_OBJECT_MISSING",
                        Map.of("storageKey", active.getStorageKey()));
            }
        }

        Set<String> trackedKeys = new HashSet<>(documentRepository.findTrackedStorageKeys());
        for (String storageKey : documentStorage.listStorageKeys("tenant/")) {
            if (!trackedKeys.contains(storageKey)) {
                orphanObjectsDetected++;
                auditService.recordSystem(
                        "DOCUMENT_INCONSISTENCY_DETECTED",
                        null,
                        null,
                        null,
                        null,
                        "ORPHAN_STORAGE_OBJECT",
                        Map.of("storageKey", storageKey));
            }
        }

        return new ReconciliationResult(
                expiredPendingMarkedFailed,
                failedRecovered,
                missingActiveObjects,
                orphanObjectsDetected);
    }
}
