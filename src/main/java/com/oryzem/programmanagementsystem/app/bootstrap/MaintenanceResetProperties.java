package com.oryzem.programmanagementsystem.app.bootstrap;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.maintenance.reset")
public record MaintenanceResetProperties(
        boolean enabled,
        String confirmation,
        boolean exitAfterReset) {

    public MaintenanceResetProperties {
        if (confirmation == null) {
            confirmation = "";
        }
    }
}
