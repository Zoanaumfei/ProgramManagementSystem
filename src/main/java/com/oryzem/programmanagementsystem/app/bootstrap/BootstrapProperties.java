package com.oryzem.programmanagementsystem.app.bootstrap;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.bootstrap")
public record BootstrapProperties(
        boolean seedData,
        InternalAdminProperties internalAdmin) {

    public BootstrapProperties {
        internalAdmin = internalAdmin != null ? internalAdmin : new InternalAdminProperties(false, null, null, false, null, null);
    }

    public record InternalAdminProperties(
            boolean enabled,
            String email,
            String displayName,
            boolean pruneOtherInternalUsers,
            String password,
            String temporaryPassword) {
    }
}
