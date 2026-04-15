package com.oryzem.programmanagementsystem.modules.documentmanagement.support;

import com.oryzem.programmanagementsystem.modules.documentmanagement.config.DocumentManagementProperties;
import com.oryzem.programmanagementsystem.platform.shared.BusinessRuleException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class DocumentFilePolicy {

    private static final Set<String> BLOCKED_EXTENSIONS = Set.of(
            "exe", "msi", "bat", "cmd", "sh", "ps1", "js", "mjs", "jar", "com", "scr",
            "html", "htm", "svg", "zip", "rar", "7z", "dwg", "dxf", "step", "stp", "igs", "iges");

    private static final Map<String, Set<String>> ALLOWED_CONTENT_TYPES = Map.ofEntries(
            Map.entry("pdf", Set.of("application/pdf")),
            Map.entry("doc", Set.of("application/msword", "application/octet-stream")),
            Map.entry("docx", Set.of("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "application/zip", "application/octet-stream")),
            Map.entry("xls", Set.of("application/vnd.ms-excel", "application/octet-stream")),
            Map.entry("xlsx", Set.of("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "application/zip", "application/octet-stream")),
            Map.entry("csv", Set.of("text/csv", "application/csv", "text/plain")),
            Map.entry("txt", Set.of("text/plain")),
            Map.entry("png", Set.of("image/png")),
            Map.entry("jpg", Set.of("image/jpeg", "image/pjpeg")),
            Map.entry("jpeg", Set.of("image/jpeg", "image/pjpeg")),
            Map.entry("webp", Set.of("image/webp", "application/octet-stream")),
            Map.entry("ppt", Set.of("application/vnd.ms-powerpoint", "application/octet-stream")),
            Map.entry("pptx", Set.of("application/vnd.openxmlformats-officedocument.presentationml.presentation", "application/zip", "application/octet-stream")));

    private final DocumentManagementProperties properties;

    public DocumentFilePolicy(DocumentManagementProperties properties) {
        this.properties = properties;
    }

    public ValidatedDocumentFile validate(
            String originalFilename,
            String declaredContentType,
            long sizeBytes,
            String checksumSha256) {
        if (sizeBytes <= 0) {
            throw new BusinessRuleException(
                    "DOCUMENT_EMPTY_FILE",
                    "Nao e permitido subir arquivos vazios.");
        }
        if (sizeBytes > properties.getMaxFileSizeBytes()) {
            throw new BusinessRuleException(
                    "DOCUMENT_FILE_TOO_LARGE",
                    "O arquivo excede o limite maximo permitido de 25 MB.",
                    Map.of("maxFileSizeBytes", properties.getMaxFileSizeBytes()));
        }

        String safeFilename = DocumentFilenameSanitizer.sanitize(originalFilename);
        String extension = extractExtension(safeFilename);
        validateDoubleExtension(safeFilename, extension);

        if (BLOCKED_EXTENSIONS.contains(extension) || !ALLOWED_CONTENT_TYPES.containsKey(extension)) {
            throw new BusinessRuleException(
                    "DOCUMENT_EXTENSION_NOT_ALLOWED",
                    "A extensao informada nao e permitida para upload.",
                    Map.of("extension", extension));
        }

        String normalizedContentType = normalizeContentType(declaredContentType);
        if (!ALLOWED_CONTENT_TYPES.get(extension).contains(normalizedContentType)) {
            throw new BusinessRuleException(
                    "DOCUMENT_CONTENT_TYPE_NOT_ALLOWED",
                    "O content type informado nao e compativel com a extensao do arquivo.",
                    Map.of("extension", extension, "contentType", normalizedContentType));
        }

        return new ValidatedDocumentFile(
                originalFilename.trim(),
                safeFilename,
                extension,
                normalizedContentType,
                sizeBytes,
                DocumentChecksumSupport.normalizeSha256(checksumSha256));
    }

    private String extractExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            throw new BusinessRuleException(
                    "DOCUMENT_EXTENSION_REQUIRED",
                    "O arquivo precisa possuir uma extensao permitida.");
        }
        return filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private void validateDoubleExtension(String filename, String extension) {
        List<String> parts = List.of(filename.toLowerCase(Locale.ROOT).split("\\."));
        if (parts.size() <= 2) {
            return;
        }
        for (int index = 0; index < parts.size() - 1; index++) {
            String part = parts.get(index);
            if (BLOCKED_EXTENSIONS.contains(part)) {
                throw new BusinessRuleException(
                        "DOCUMENT_DOUBLE_EXTENSION_BLOCKED",
                        "O nome do arquivo contem uma double extension bloqueada.",
                        Map.of("filename", filename, "blockedSegment", part, "extension", extension));
            }
        }
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            throw new BusinessRuleException(
                    "DOCUMENT_CONTENT_TYPE_REQUIRED",
                    "O content type do arquivo e obrigatorio.");
        }
        return contentType.trim().toLowerCase(Locale.ROOT);
    }

    public record ValidatedDocumentFile(
            String originalFilename,
            String safeFilename,
            String extension,
            String contentType,
            long sizeBytes,
            String checksumSha256) {
    }
}
