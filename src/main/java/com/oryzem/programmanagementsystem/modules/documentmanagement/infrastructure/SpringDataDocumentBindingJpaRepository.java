package com.oryzem.programmanagementsystem.modules.documentmanagement.infrastructure;

import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentContextType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataDocumentBindingJpaRepository extends JpaRepository<DocumentBindingEntity, String> {

    Optional<DocumentBindingEntity> findByDocumentId(String documentId);

    List<DocumentBindingEntity> findAllByContextTypeAndContextIdOrderByCreatedAtDesc(
            DocumentContextType contextType,
            String contextId);

    @Query(value = """
            select count(*)
            from document_binding b
            join document d on d.id = b.document_id
            where b.context_type = :contextType
              and b.context_id = :contextId
              and d.status <> 'DELETED'
              and d.deleted_at is null
            """, nativeQuery = true)
    long countTrackedByContext(
            @Param("contextType") String contextType,
            @Param("contextId") String contextId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from DocumentBindingEntity b where b.documentId in :documentIds")
    void deleteAllByDocumentIdIn(@Param("documentIds") Collection<String> documentIds);
}
