package com.oryzem.programmanagementsystem.modules.projectmanagement;

import com.oryzem.programmanagementsystem.platform.authorization.TenantType;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

record MilestoneTemplateResponse(
        String id,
        String name,
        String description,
        MilestoneTemplateStatus status,
        List<MilestoneTemplateItemResponse> items,
        Instant createdAt,
        Instant updatedAt) {

    static MilestoneTemplateResponse from(MilestoneTemplateEntity template) {
        return new MilestoneTemplateResponse(
                template.getId(),
                template.getName(),
                template.getDescription(),
                template.getStatus(),
                template.getItems().stream()
                        .sorted(Comparator.comparing(MilestoneTemplateItemEntity::getSortOrder))
                        .map(MilestoneTemplateItemResponse::from)
                        .toList(),
                template.getCreatedAt(),
                template.getUpdatedAt());
    }
}

record MilestoneTemplateItemResponse(
        String id,
        String name,
        int sortOrder,
        boolean required,
        Integer offsetWeeks) {

    static MilestoneTemplateItemResponse from(MilestoneTemplateItemEntity item) {
        return new MilestoneTemplateItemResponse(
                item.getId(),
                item.getName(),
                item.getSortOrder(),
                item.isRequired(),
                item.getOffsetWeeks());
    }
}


