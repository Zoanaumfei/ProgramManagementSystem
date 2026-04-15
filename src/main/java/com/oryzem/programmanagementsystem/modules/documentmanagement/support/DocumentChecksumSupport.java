package com.oryzem.programmanagementsystem.modules.documentmanagement.support;

import com.oryzem.programmanagementsystem.platform.shared.BusinessRuleException;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

public final class DocumentChecksumSupport {

    private DocumentChecksumSupport() {
    }

    public static String normalizeSha256(String checksumSha256) {
        if (checksumSha256 == null || checksumSha256.isBlank()) {
            throw new BusinessRuleException(
                    "DOCUMENT_CHECKSUM_REQUIRED",
                    "O checksum SHA-256 do arquivo e obrigatorio.");
        }
        String normalized = checksumSha256.trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("^[a-f0-9]{64}$")) {
            throw new BusinessRuleException(
                    "DOCUMENT_CHECKSUM_INVALID",
                    "O checksum SHA-256 informado e invalido.");
        }
        return normalized;
    }

    public static String sha256Hex(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is not available.", exception);
        }
    }

    public static String sha256Hex(InputStream inputStream) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) >= 0) {
                if (read > 0) {
                    digest.update(buffer, 0, read);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read content for SHA-256 calculation.", exception);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is not available.", exception);
        }
    }
}
