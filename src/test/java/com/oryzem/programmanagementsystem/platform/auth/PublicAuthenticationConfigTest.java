package com.oryzem.programmanagementsystem.platform.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;

class PublicAuthenticationConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PublicAuthenticationConfig.class))
            .withPropertyValues(
                    "app.security.cognito.issuer-uri=https://cognito-idp.sa-east-1.amazonaws.com/sa-east-1_example",
                    "app.security.cognito.jwk-set-uri=https://cognito-idp.sa-east-1.amazonaws.com/sa-east-1_example/.well-known/jwks.json",
                    "app.security.cognito.app-client-id=test-client",
                    "app.security.cognito.allowed-origins[0]=http://localhost:3000");

    @Test
    void providerCognitoSelectsCognitoGateway() {
        contextRunner
                .withUserConfiguration(CognitoClientTestConfiguration.class)
                .withPropertyValues("app.security.identity.provider=cognito")
                .run(context -> {
                    assertThat(context).hasSingleBean(PublicAuthenticationGateway.class);
                    assertThat(context.getBean(PublicAuthenticationGateway.class))
                            .isInstanceOf(CognitoPublicAuthenticationGateway.class);
                });
    }

    @Test
    void providerStubSelectsStubGateway() {
        contextRunner
                .withPropertyValues("app.security.identity.provider=stub")
                .run(context -> {
                    assertThat(context).hasSingleBean(PublicAuthenticationGateway.class);
                    assertThat(context.getBean(PublicAuthenticationGateway.class))
                            .isInstanceOf(StubPublicAuthenticationGateway.class);
                });
    }

    @Test
    void providerCognitoFailsFastWhenClientBeanIsMissing() {
        contextRunner
                .withUserConfiguration(CognitoPropertiesOnlyTestConfiguration.class)
                .withPropertyValues("app.security.identity.provider=cognito")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasMessageContaining("Cognito public authentication requires a CognitoIdentityProviderClient bean.");
                });
    }

    @Configuration
    @EnableConfigurationProperties(CognitoProperties.class)
    static class CognitoClientTestConfiguration {

        @Bean(destroyMethod = "close")
        CognitoIdentityProviderClient cognitoIdentityProviderClient() {
            return CognitoIdentityProviderClient.builder()
                    .region(Region.SA_EAST_1)
                    .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
                    .build();
        }
    }

    @Configuration
    @EnableConfigurationProperties(CognitoProperties.class)
    static class CognitoPropertiesOnlyTestConfiguration {
    }
}
