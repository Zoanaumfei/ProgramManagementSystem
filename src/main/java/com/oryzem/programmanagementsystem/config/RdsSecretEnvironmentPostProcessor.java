package com.oryzem.programmanagementsystem.config;

import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.util.StringUtils;

public class RdsSecretEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    static final String PROPERTY_SOURCE_NAME = "programManagementSystemRdsSecret";

    private static final Logger LOGGER = LoggerFactory.getLogger(RdsSecretEnvironmentPostProcessor.class);

    private final RdsSecretProvider secretProvider;

    public RdsSecretEnvironmentPostProcessor() {
        this(new AwsSecretsManagerRdsSecretProvider());
    }

    RdsSecretEnvironmentPostProcessor(RdsSecretProvider secretProvider) {
        this.secretProvider = secretProvider;
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!environment.getProperty("app.rds.secret.enabled", Boolean.class, false)) {
            return;
        }

        String secretId = environment.getProperty("app.rds.secret.secret-id");
        String region = environment.getProperty("app.rds.secret.region", "sa-east-1");
        boolean failFast = environment.getProperty("app.rds.secret.fail-fast", Boolean.class, true);

        if (!StringUtils.hasText(secretId)) {
            throw new IllegalStateException("The RDS secret loader is enabled, but no secret id was configured.");
        }

        try {
            Map<String, Object> secretValues = secretProvider.load(secretId, region);
            Map<String, Object> resolvedProperties = buildResolvedProperties(secretValues);
            addPropertySource(environment.getPropertySources(), new MapPropertySource(PROPERTY_SOURCE_NAME, resolvedProperties));
            LOGGER.info("Loaded RDS datasource properties from AWS Secrets Manager secret '{}'.", secretId);
        } catch (RuntimeException exception) {
            if (failFast) {
                throw new IllegalStateException(
                        "Unable to load the RDS secret '%s' from region '%s'."
                                .formatted(secretId, region),
                        exception);
            }
            LOGGER.warn(
                    "Unable to load the RDS secret '{}' from region '{}'. The application will keep the existing datasource properties.",
                    secretId,
                    region,
                    exception);
        }
    }

    @Override
    public int getOrder() {
        return ConfigDataEnvironmentPostProcessor.ORDER + 1;
    }

    private Map<String, Object> buildResolvedProperties(Map<String, Object> secretValues) {
        Map<String, Object> properties = new LinkedHashMap<>();

        properties.put("DB_PASSWORD", requiredString(secretValues, "password"));
        putIfPresent(properties, "DB_USERNAME", firstString(secretValues, "username", "user"));

        String jdbcUrl = firstString(secretValues, "url", "jdbcUrl");
        if (StringUtils.hasText(jdbcUrl)) {
            properties.put("DB_URL", jdbcUrl);
            return properties;
        }

        String host = firstString(secretValues, "host", "hostname");
        Integer port = firstInteger(secretValues, "port");
        String database = firstString(secretValues, "dbname", "database", "dbName");
        if (StringUtils.hasText(host) && port != null && StringUtils.hasText(database)) {
            properties.put("DB_URL", "jdbc:postgresql://%s:%d/%s".formatted(host, port, database));
        }

        return properties;
    }

    private void addPropertySource(MutablePropertySources propertySources, MapPropertySource propertySource) {
        if (propertySources.contains(PROPERTY_SOURCE_NAME)) {
            propertySources.replace(PROPERTY_SOURCE_NAME, propertySource);
            return;
        }

        if (propertySources.contains(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME)) {
            propertySources.addAfter(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, propertySource);
            return;
        }

        if (propertySources.contains(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME)) {
            propertySources.addAfter(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME, propertySource);
            return;
        }

        propertySources.addFirst(propertySource);
    }

    private String requiredString(Map<String, Object> values, String key) {
        String value = firstString(values, key);
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException("The RDS secret payload must include a non-empty '%s' field."
                    .formatted(key));
        }
        return value;
    }

    private void putIfPresent(Map<String, Object> properties, String propertyName, String value) {
        if (StringUtils.hasText(value)) {
            properties.put(propertyName, value);
        }
    }

    private String firstString(Map<String, Object> values, String... keys) {
        for (String key : keys) {
            Object value = values.get(key);
            if (value instanceof String stringValue && StringUtils.hasText(stringValue)) {
                return stringValue;
            }
        }
        return null;
    }

    private Integer firstInteger(Map<String, Object> values, String... keys) {
        for (String key : keys) {
            Object value = values.get(key);
            if (value instanceof Number numberValue) {
                return numberValue.intValue();
            }
            if (value instanceof String stringValue && StringUtils.hasText(stringValue)) {
                return Integer.parseInt(stringValue);
            }
        }
        return null;
    }
}
