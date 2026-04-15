package com.oryzem.programmanagementsystem.modules.documentmanagement.infrastructure;

import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SpringDataDocumentJpaRepository extends JpaRepository<DocumentEntity, String> {

    List<DocumentEntity> findAllByIdIn(Collection<String> ids);

    List<DocumentEntity> findAllByStatusAndUploadExpiresAtBefore(DocumentStatus status, Instant cutoff);

    List<DocumentEntity> findAllByStatus(DocumentStatus status);

    @Query("""
            select d.storageKey
            from DocumentEntity d
            where d.deletedAt is null
              and d.status <> com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentStatus.DELETED
            """)
    List<String> findTrackedStorageKeys();
}
