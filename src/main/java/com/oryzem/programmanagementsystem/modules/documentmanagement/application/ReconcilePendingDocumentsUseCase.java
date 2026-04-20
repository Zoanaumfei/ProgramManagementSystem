package com.oryzem.programmanagementsystem.modules.documentmanagement.application;

import com.oryzem.programmanagementsystem.modules.documentmanagement.application.model.DocumentBindingRecord;
import com.oryzem.programmanagementsystem.modules.documentmanagement.application.model.DocumentRecord;
import com.oryzem.programmanagementsystem.modules.documentmanagement.application.port.DocumentBindingRepository;
import com.oryzem.programmanagementsystem.modules.documentmanagement.application.port.DocumentRepository;
import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentStatus;
import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentStorage;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReconcilePendingDocumentsUseCase {

    private final DocumentRepository documentRepository;
    private final DocumentBindingRepository bindingRepository;
    private final DocumentStorage documentStorage;
    private final DocumentAuditService auditService;

    public ReconcilePendingDocumentsUseCase(
            DocumentRepository documentRepository,
            DocumentBindingRepository bindingRepository,
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

        Map<String, DocumentBindingRecord> bindingsByDocumentId = new HashMap<>();
        bindingRepository.findAll().forEach(binding -> bindingsByDocumentId.put(binding.documentId(), binding));

        for (DocumentRecord pending : documentRepository.findAllByStatusAndUploadExpiresAtBefore(DocumentStatus.PENDING_UPLOAD, now)) {
            if (!documentStorage.headObject(pending.storageKey()).exists()) {
                pending.markFailed(now);
                documentRepository.save(pending);
                expiredPendingMarkedFailed++;
                DocumentBindingRecord binding = bindingsByDocumentId.get(pending.id());
                auditService.recordSystem(
                        "DOCUMENT_INCONSISTENCY_DETECTED",
                        pending.tenantId(),
                        pending.id(),
                        binding != null ? binding.contextType() : null,
                        binding != null ? binding.contextId() : null,
                        "PENDING_EXPIRED",
                        Map.of("storageKey", pending.storageKey()));
            }
        }

        for (DocumentRecord failed : documentRepository.findAllByStatus(DocumentStatus.FAILED)) {
            if (documentStorage.headObject(failed.storageKey()).exists()) {
                failed.markActive(now);
                documentRepository.save(failed);
                failedRecovered++;
                DocumentBindingRecord binding = bindingsByDocumentId.get(failed.id());
                auditService.recordSystem(
                        "DOCUMENT_UPLOAD_FINALIZED",
                        failed.tenantId(),
                        failed.id(),
                        binding != null ? binding.contextType() : null,
                        binding != null ? binding.contextId() : null,
                        "RECOVERED",
                        Map.of("storageKey", failed.storageKey()));
            }
        }

        for (DocumentRecord active : documentRepository.findAllByStatus(DocumentStatus.ACTIVE)) {
            if (!documentStorage.headObject(active.storageKey()).exists()) {
                missingActiveObjects++;
                DocumentBindingRecord binding = bindingsByDocumentId.get(active.id());
                auditService.recordSystem(
                        "DOCUMENT_INCONSISTENCY_DETECTED",
                        active.tenantId(),
                        active.id(),
                        binding != null ? binding.contextType() : null,
                        binding != null ? binding.contextId() : null,
                        "ACTIVE_OBJECT_MISSING",
                        Map.of("storageKey", active.storageKey()));
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