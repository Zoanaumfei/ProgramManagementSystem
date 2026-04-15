package com.oryzem.programmanagementsystem.modules.documentmanagement.config;

import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentStorage;
import com.oryzem.programmanagementsystem.modules.documentmanagement.storage.NoopDocumentStorage;
import com.oryzem.programmanagementsystem.modules.documentmanagement.storage.S3DocumentStorage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DocumentManagementStorageConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "app.document-management.storage.s3", name = "bucket")
    DocumentStorage s3DocumentStorage(DocumentManagementProperties properties) {
        if (properties.getStorage().getS3().getBucket() == null || properties.getStorage().getS3().getBucket().isBlank()) {
            return new NoopDocumentStorage();
        }
        return new S3DocumentStorage(properties);
    }

    @Bean
    @ConditionalOnMissingBean(DocumentStorage.class)
    DocumentStorage fallbackDocumentStorage() {
        return new NoopDocumentStorage();
    }
}
