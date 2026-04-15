package com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure.mapper;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.model.ProjectIdempotencyRecord;
import com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure.ProjectIdempotencyEntity;
import org.springframework.stereotype.Component;

@Component
public class ProjectIdempotencyPersistenceMapper {

    public ProjectIdempotencyRecord toRecord(ProjectIdempotencyEntity entity) {
        return new ProjectIdempotencyRecord(
                entity.getIdempotencyKey(),
                entity.getTenantId(),
                entity.getOperation(),
                entity.getRequestHash(),
                entity.getResponsePayload(),
                entity.getCreatedAt());
    }

    public ProjectIdempotencyEntity toEntity(ProjectIdempotencyRecord record) {
        return ProjectIdempotencyEntity.create(
                record.idempotencyKey(),
                record.tenantId(),
                record.operation(),
                record.requestHash(),
                record.responsePayload(),
                record.createdAt());
    }
}
