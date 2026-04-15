package com.oryzem.programmanagementsystem.modules.documentmanagement.infrastructure;

import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentContextType;
import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentStatus;
import java.time.Instant;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentStateModelTest {

    @Test
    void shouldTransitionDocumentStatusAndCreateBinding() {
        Instant now = Instant.parse("2026-04-08T12:00:00Z");
        DocumentEntity document = DocumentEntity.initiate(
                "DOC-001",
                "TEN-tenant-a",
                "arquivo.pdf",
                "arquivo.pdf",
                "application/pdf",
                "pdf",
                1234,
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                "S3",
                "tenant/TEN-tenant-a/PROJECT/project-1/DOC-001/object",
                "USR-1",
                "tenant-a",
                now.plusSeconds(60),
                now);

        DocumentBindingEntity binding = DocumentBindingEntity.create(
                "DBN-001",
                document.getId(),
                DocumentContextType.PROJECT,
                "project-1",
                "tenant-a",
                "USR-1",
                now);

        document.markActive(now.plusSeconds(5));
        document.markDeleted(now.plusSeconds(10));

        assertThat(binding.getDocumentId()).isEqualTo("DOC-001");
        assertThat(binding.getContextType()).isEqualTo(DocumentContextType.PROJECT);
        assertThat(document.getStatus()).isEqualTo(DocumentStatus.DELETED);
        assertThat(document.getDeletedAt()).isEqualTo(now.plusSeconds(10));
    }
}
