package com.oryzem.programmanagementsystem.modules.documentmanagement.support;

import com.oryzem.programmanagementsystem.platform.shared.BusinessRuleException;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;

@Component
public class DocumentFileSignatureValidator {

    public void validate(String extension, byte[] signatureBytes) {
        if (!requiresSignatureValidation(extension)) {
            return;
        }
        if (signatureBytes == null || signatureBytes.length == 0 || !matches(extension, signatureBytes)) {
            throw new BusinessRuleException(
                    "DOCUMENT_SIGNATURE_INVALID",
                    "A assinatura binaria real do arquivo nao corresponde ao tipo permitido.");
        }
    }

    boolean requiresSignatureValidation(String extension) {
        return switch (extension == null ? "" : extension.toLowerCase()) {
            case "pdf", "png", "jpg", "jpeg", "webp", "doc", "xls", "ppt", "docx", "xlsx", "pptx" -> true;
            default -> false;
        };
    }

    private boolean matches(String extension, byte[] bytes) {
        return switch (extension.toLowerCase()) {
            case "pdf" -> startsWith(bytes, 0x25, 0x50, 0x44, 0x46, 0x2D);
            case "png" -> startsWith(bytes, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A);
            case "jpg", "jpeg" -> startsWith(bytes, 0xFF, 0xD8, 0xFF);
            case "webp" -> asciiStartsWith(bytes, "RIFF") && asciiAt(bytes, 8, "WEBP");
            case "doc", "xls", "ppt" -> startsWith(bytes, 0xD0, 0xCF, 0x11, 0xE0, 0xA1, 0xB1, 0x1A, 0xE1);
            case "docx", "xlsx", "pptx" -> startsWith(bytes, 0x50, 0x4B, 0x03, 0x04)
                    || startsWith(bytes, 0x50, 0x4B, 0x05, 0x06)
                    || startsWith(bytes, 0x50, 0x4B, 0x07, 0x08);
            default -> true;
        };
    }

    private boolean startsWith(byte[] bytes, int... expected) {
        if (bytes.length < expected.length) {
            return false;
        }
        for (int index = 0; index < expected.length; index++) {
            if ((bytes[index] & 0xFF) != expected[index]) {
                return false;
            }
        }
        return true;
    }

    private boolean asciiStartsWith(byte[] bytes, String expected) {
        byte[] expectedBytes = expected.getBytes(StandardCharsets.US_ASCII);
        if (bytes.length < expectedBytes.length) {
            return false;
        }
        for (int index = 0; index < expectedBytes.length; index++) {
            if (bytes[index] != expectedBytes[index]) {
                return false;
            }
        }
        return true;
    }

    private boolean asciiAt(byte[] bytes, int offset, String expected) {
        byte[] expectedBytes = expected.getBytes(StandardCharsets.US_ASCII);
        if (bytes.length < offset + expectedBytes.length) {
            return false;
        }
        for (int index = 0; index < expectedBytes.length; index++) {
            if (bytes[offset + index] != expectedBytes[index]) {
                return false;
            }
        }
        return true;
    }
}
