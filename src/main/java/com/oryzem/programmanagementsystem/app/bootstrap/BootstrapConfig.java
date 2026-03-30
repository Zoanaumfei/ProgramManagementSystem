package com.oryzem.programmanagementsystem.app.bootstrap;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({BootstrapProperties.class, MaintenanceResetProperties.class})
class BootstrapConfig {
}
