package com.oryzem.programmanagementsystem.modules.projectmanagement.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oryzem.programmanagementsystem.platform.shared.BusinessRuleException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.stereotype.Component;

@Component
public class ProjectRequestHashSupport {

    private final ObjectMapper objectMapper;

    public ProjectRequestHashSupport(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String hash(Object value) {
        try {
            byte[] payload = objectMapper.writeValueAsBytes(value);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(payload);
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte current : bytes) {
                hex.append(String.format("%02x", current));
            }
            return hex.toString();
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize project request payload for idempotency.", exception);
        } catch (NoSuchAlgorithmException exception) {
            throw new BusinessRuleException(
                    "PROJECT_HASHING_UNAVAILABLE",
                    "SHA-256 is not available for idempotency hashing.");
        }
    }
}
