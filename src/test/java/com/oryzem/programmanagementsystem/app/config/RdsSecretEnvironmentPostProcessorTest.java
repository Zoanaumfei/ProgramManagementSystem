package com.oryzem.programmanagementsystem.app.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

class RdsSecretEnvironmentPostProcessorTest {

    @Test
    void shouldLoadDatasourcePropertiesFromSecretsManager() {
        StandardEnvironment environment = environmentWithSecretLoaderEnabled();
        RdsSecretEnvironmentPostProcessor processor = new RdsSecretEnvironmentPostProcessor(
                (secretId, region) -> Map.of(
                        "host", "db.example.internal",
                        "port", 5432,
                        "dbname", "program_management_system",
                        "username", "pms_admin",
                        "password", "secret-password"));

        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("DB_URL"))
                .isEqualTo("jdbc:postgresql://db.example.internal:5432/program_management_system");
        assertThat(environment.getProperty("DB_USERNAME")).isEqualTo("pms_admin");
        assertThat(environment.getProperty("DB_PASSWORD")).isEqualTo("secret-password");
    }

    @Test
    void shouldKeepExplicitEnvironmentVariablesAheadOfSecretValues() {
        StandardEnvironment environment = environmentWithSecretLoaderEnabled();
        environment.getPropertySources().replace(
                StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                new MapPropertySource(
                        StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                        Map.of("DB_USERNAME", "override-user")));

        RdsSecretEnvironmentPostProcessor processor = new RdsSecretEnvironmentPostProcessor(
                (secretId, region) -> Map.of(
                        "username", "pms_admin",
                        "password", "secret-password"));

        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("DB_USERNAME")).isEqualTo("override-user");
        assertThat(environment.getProperty("DB_PASSWORD")).isEqualTo("secret-password");
    }

    @Test
    void shouldAllowPartialSecretWhenUrlComesFromProfileDefaults() {
        StandardEnvironment environment = environmentWithSecretLoaderEnabled();
        environment.getPropertySources().addLast(new MapPropertySource(
                "profileDefaults",
                Map.of("DB_URL", "jdbc:postgresql://program-management-system-db:5432/program_management_system")));

        RdsSecretEnvironmentPostProcessor processor = new RdsSecretEnvironmentPostProcessor(
                (secretId, region) -> Map.of(
                        "username", "pms_admin",
                        "password", "secret-password"));

        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("DB_URL"))
                .isEqualTo("jdbc:postgresql://program-management-system-db:5432/program_management_system");
        assertThat(environment.getProperty("DB_PASSWORD")).isEqualTo("secret-password");
    }

    @Test
    void shouldFailFastWhenPasswordIsMissing() {
        StandardEnvironment environment = environmentWithSecretLoaderEnabled();
        RdsSecretEnvironmentPostProcessor processor = new RdsSecretEnvironmentPostProcessor(
                (secretId, region) -> Map.of("username", "pms_admin"));

        assertThatThrownBy(() -> processor.postProcessEnvironment(environment, new SpringApplication()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unable to load the RDS secret");
    }

    private StandardEnvironment environmentWithSecretLoaderEnabled() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addLast(new MapPropertySource(
                "rdsSecretConfig",
                Map.of(
                        "app.rds.secret.enabled", "true",
                        "app.rds.secret.secret-id", "program-management-system/rds/master",
                        "app.rds.secret.region", "sa-east-1")));
        return environment;
    }
}
