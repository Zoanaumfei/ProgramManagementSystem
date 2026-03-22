package com.oryzem.programmanagementsystem.app.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

class AwsSecretsManagerRdsSecretProvider implements RdsSecretProvider {

    private static final TypeReference<Map<String, Object>> SECRET_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Map<String, Object> load(String secretId, String region) {
        try (SecretsManagerClient client = SecretsManagerClient.builder()
                .region(Region.of(region))
                .build()) {
            GetSecretValueResponse response = client.getSecretValue(request -> request.secretId(secretId));
            return parseSecret(secretId, response.secretString());
        }
    }

    private Map<String, Object> parseSecret(String secretId, String secretString) {
        if (secretString == null || secretString.isBlank()) {
            throw new IllegalStateException("Secrets Manager secret '%s' does not contain a SecretString payload."
                    .formatted(secretId));
        }

        try {
            return objectMapper.readValue(secretString, SECRET_TYPE);
        } catch (IOException exception) {
            throw new UncheckedIOException(
                    "Unable to parse the JSON payload from Secrets Manager secret '%s'."
                            .formatted(secretId),
                    exception);
        }
    }
}
