package com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure;

import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectFrameworkAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectFrameworkUiLayout;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "project_framework")
public class ProjectFrameworkEntity {

    @Id
    @Column(length = 64, nullable = false)
    private String id;

    @Column(length = 64, nullable = false, unique = true)
    private String code;

    @Column(name = "display_name", length = 160, nullable = false)
    private String displayName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "ui_layout", length = 32, nullable = false)
    private ProjectFrameworkUiLayout uiLayout;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ProjectFrameworkEntity() {
    }

    static ProjectFrameworkEntity from(ProjectFrameworkAggregate aggregate) {
        ProjectFrameworkEntity entity = new ProjectFrameworkEntity();
        entity.id = aggregate.id();
        entity.code = aggregate.code();
        entity.displayName = aggregate.displayName();
        entity.description = aggregate.description();
        entity.uiLayout = aggregate.uiLayout();
        entity.active = aggregate.active();
        entity.createdAt = aggregate.createdAt();
        entity.updatedAt = aggregate.updatedAt();
        return entity;
    }

    ProjectFrameworkAggregate toDomain() {
        return new ProjectFrameworkAggregate(id, code, displayName, description, uiLayout, active, createdAt, updatedAt);
    }
}
