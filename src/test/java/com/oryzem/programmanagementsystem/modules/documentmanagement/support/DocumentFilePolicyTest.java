package com.oryzem.programmanagementsystem.modules.documentmanagement.support;

import com.oryzem.programmanagementsystem.modules.documentmanagement.config.DocumentManagementProperties;
import com.oryzem.programmanagementsystem.platform.shared.BusinessRuleException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentFilePolicyTest {

    private final DocumentFilePolicy policy = new DocumentFilePolicy(new DocumentManagementProperties());

    @Test
    void shouldAcceptAllowedExtensionAndContentType() {
        DocumentFilePolicy.ValidatedDocumentFile validated = policy.validate(
                "Relatorio Final.pdf",
                "application/pdf",
                1024,
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");

        assertThat(validated.safeFilename()).isEqualTo("Relatorio-Final.pdf");
        assertThat(validated.extension()).isEqualTo("pdf");
    }

    @Test
    void shouldBlockDoubleExtensionWithRiskySegment() {
        assertThatThrownBy(() -> policy.validate(
                "evidencia.exe.pdf",
                "application/pdf",
                1024,
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("double extension");
    }

    @Test
    void shouldBlockFilesAboveConfiguredLimit() {
        assertThatThrownBy(() -> policy.validate(
                "grande.pdf",
                "application/pdf",
                26L * 1024L * 1024L,
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("25 MB");
    }
}
