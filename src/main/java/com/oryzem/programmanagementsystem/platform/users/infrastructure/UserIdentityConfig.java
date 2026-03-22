package com.oryzem.programmanagementsystem.platform.users.infrastructure;

import com.oryzem.programmanagementsystem.platform.users.domain.UserIdentityGateway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;

@Configuration
@EnableConfigurationProperties(UserIdentityProperties.class)
class UserIdentityConfig {

    @Bean
    @ConditionalOnProperty(prefix = "app.security.identity", name = "provider", havingValue = "cognito")
    UserIdentityGateway cognitoUserIdentityGateway(UserIdentityProperties properties) {
        if (!StringUtils.hasText(properties.userPoolId())) {
            throw new IllegalStateException("Cognito identity integration requires app.security.identity.user-pool-id.");
        }

        CognitoIdentityProviderClient client = CognitoIdentityProviderClient.builder()
                .region(Region.of(properties.region()))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
        return new CognitoUserIdentityGateway(properties, client);
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.security.identity", name = "provider", havingValue = "stub", matchIfMissing = true)
    StubUserIdentityGateway stubUserIdentityGateway() {
        return new StubUserIdentityGateway();
    }
}

