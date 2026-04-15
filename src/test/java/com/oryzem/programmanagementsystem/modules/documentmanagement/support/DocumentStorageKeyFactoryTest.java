package com.oryzem.programmanagementsystem.modules.documentmanagement.support;

import com.oryzem.programmanagementsystem.modules.documentmanagement.config.DocumentManagementProperties;
import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentContextType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentStorageKeyFactoryTest {

    private final DocumentStorageKeyFactory factory = new DocumentStorageKeyFactory(new DocumentManagementProperties());

    @Test
    void shouldCreateOpaqueStorageKeyWithExpectedPrefix() {
        String storageKey = factory.create("TEN-tenant-a", DocumentContextType.PROJECT, "project/123", "DOC-001");

        assertThat(storageKey).startsWith("tenant/TEN-tenant-a/PROJECT/project_123/DOC-001/");
    }
}
