package com.oryzem.programmanagementsystem.modules.documentmanagement.support;

import com.oryzem.programmanagementsystem.modules.documentmanagement.config.DocumentManagementProperties;
import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentContextType;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class DocumentStorageKeyFactory {

    private final DocumentManagementProperties properties;

    public DocumentStorageKeyFactory(DocumentManagementProperties properties) {
        this.properties = properties;
    }

    public String create(String tenantId, DocumentContextType contextType, String contextId, String documentId) {
        String keyPrefix = sanitizeSegment(properties.getStorage().getS3().getKeyPrefix());
        return keyPrefix
                + "/" + sanitizeSegment(tenantId)
                + "/" + contextType.name()
                + "/" + sanitizeSegment(contextId)
                + "/" + sanitizeSegment(documentId)
                + "/" + UUID.randomUUID();
    }

    private String sanitizeSegment(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.trim().replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
