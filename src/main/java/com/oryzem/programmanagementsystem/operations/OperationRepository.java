package com.oryzem.programmanagementsystem.operations;

import java.util.List;
import java.util.Optional;

public interface OperationRepository {

    List<OperationRecord> findAll();

    List<OperationRecord> findByTenantId(String tenantId);

    Optional<OperationRecord> findById(String operationId);

    OperationRecord save(OperationRecord operation);

    void deleteById(String operationId);

    void deleteAll();
}
