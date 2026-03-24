package com.oryzem.programmanagementsystem.platform.auth;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;

@Configuration
class PublicAuthenticationConfig {

    @Bean
    @ConditionalOnProperty(prefix = "app.security.identity", name = "provider", havingValue = "cognito")
    PublicAuthenticationGateway cognitoPublicAuthenticationGateway(
            CognitoProperties cognitoProperties,
            ObjectProvider<CognitoIdentityProviderClient> clientProvider) {
        // `@ConditionalOnBean` is unreliable in regular `@Configuration` classes because
        // evaluation order may skip this bean before the Cognito client is registered.
        CognitoIdentityProviderClient client = clientProvider.getIfAvailable();
        if (client == null) {
            throw new IllegalStateException(
                    "Cognito public authentication requires a CognitoIdentityProviderClient bean.");
        }
        return new CognitoPublicAuthenticationGateway(cognitoProperties, client);
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.security.identity", name = "provider", havingValue = "stub", matchIfMissing = true)
    PublicAuthenticationGateway stubPublicAuthenticationGateway() {
        return new StubPublicAuthenticationGateway();
    }
}
