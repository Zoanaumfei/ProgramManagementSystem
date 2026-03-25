package com.oryzem.programmanagementsystem.platform.users.deprecation;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(LegacyUsersFeatureFlagsProperties.class)
public class LegacyUsersFeatureFlagConfig {
}
