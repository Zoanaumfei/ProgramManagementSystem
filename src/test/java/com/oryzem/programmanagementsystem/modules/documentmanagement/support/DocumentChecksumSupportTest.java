package com.oryzem.programmanagementsystem.modules.documentmanagement.support;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentChecksumSupportTest {

    @Test
    void shouldCalculateSha256Hex() {
        String checksum = DocumentChecksumSupport.sha256Hex("oryzem".getBytes(StandardCharsets.UTF_8));

        assertThat(checksum).isEqualTo("4f61d1ced265da98b3645a732a8af662c7170d897be0b508a20a1297e191eadd");
    }

    @Test
    void shouldNormalizeProvidedChecksum() {
        String normalized = DocumentChecksumSupport.normalizeSha256(
                "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");

        assertThat(normalized).isEqualTo("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
    }
}
