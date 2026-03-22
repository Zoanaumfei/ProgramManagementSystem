package com.oryzem.programmanagementsystem.modules.operations;

import com.oryzem.programmanagementsystem.modules.reports.ReportOperationQueryPort;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
class ReportOperationQueryAdapter implements ReportOperationQueryPort {

    private final OperationRepository operationRepository;

    ReportOperationQueryAdapter(OperationRepository operationRepository) {
        this.operationRepository = operationRepository;
    }

    @Override
    public List<ReportOperationView> findOperations(String tenantId, String statusFilter) {
        List<OperationRecord> operations = tenantId == null
                ? operationRepository.findAll()
                : operationRepository.findByTenantId(tenantId);
        String normalizedStatus = statusFilter == null || statusFilter.isBlank()
                ? null
                : statusFilter.trim().toUpperCase(Locale.ROOT);
        return operations.stream()
                .filter(operation -> normalizedStatus == null || normalizedStatus.equals(operation.status().name()))
                .map(operation -> new ReportOperationView(
                        operation.id(),
                        operation.title(),
                        operation.description(),
                        operation.tenantId(),
                        operation.tenantType(),
                        operation.createdBy(),
                        operation.status().name(),
                        operation.createdAt(),
                        operation.updatedAt()))
                .toList();
    }
}
