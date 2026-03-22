package com.oryzem.programmanagementsystem.modules.projectmanagement;

import com.oryzem.programmanagementsystem.platform.authorization.TenantType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@MappedSuperclass
abstract class JpaPortfolioAuditableEntity extends PortfolioAuditableEntity {

    @Id
    @Column(length = 64, nullable = false)
    @Override
    public String getId() {
        return super.getId();
    }

    @Column(name = "created_at", nullable = false)
    @Override
    public Instant getCreatedAt() {
        return super.getCreatedAt();
    }

    @Column(name = "updated_at", nullable = false)
    @Override
    public Instant getUpdatedAt() {
        return super.getUpdatedAt();
    }

    @Column(name = "created_by", nullable = false, length = 128)
    @Override
    public String getCreatedBy() {
        return super.getCreatedBy();
    }

    @Column(name = "updated_by", nullable = false, length = 128)
    @Override
    public String getUpdatedBy() {
        return super.getUpdatedBy();
    }
}



