package com.oryzem.programmanagementsystem.modules.documentmanagement.support;

import com.oryzem.programmanagementsystem.platform.shared.BusinessRuleException;
import java.text.Normalizer;

public final class DocumentFilenameSanitizer {

    private DocumentFilenameSanitizer() {
    }

    public static String sanitize(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new BusinessRuleException(
                    "DOCUMENT_FILENAME_REQUIRED",
                    "O nome original do arquivo e obrigatorio.");
        }

        String leafName = originalFilename.replace('\\', '/').trim();
        int slashIndex = leafName.lastIndexOf('/');
        if (slashIndex >= 0) {
            leafName = leafName.substring(slashIndex + 1);
        }

        String normalized = Normalizer.normalize(leafName, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("[\\r\\n\\t]", " ")
                .replaceAll("[^A-Za-z0-9._ -]", "_")
                .replace(' ', '-')
                .replaceAll("-+", "-")
                .replaceAll("_+", "_")
                .replaceAll("\\.{2,}", ".")
                .replaceAll("^-+", "")
                .replaceAll("^_+", "");

        if (normalized.isBlank() || ".".equals(normalized) || "..".equals(normalized)) {
            throw new BusinessRuleException(
                    "DOCUMENT_FILENAME_UNSAFE",
                    "O nome informado para o arquivo nao e seguro.");
        }

        if (normalized.length() > 255) {
            normalized = normalized.substring(0, 255);
        }
        return normalized;
    }
}
