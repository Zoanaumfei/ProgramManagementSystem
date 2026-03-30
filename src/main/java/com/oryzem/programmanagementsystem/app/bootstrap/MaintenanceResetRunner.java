package com.oryzem.programmanagementsystem.app.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.maintenance.reset", name = "enabled", havingValue = "true")
class MaintenanceResetRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MaintenanceResetRunner.class);
    private static final String REQUIRED_CONFIRMATION = "RESET_RDS_SAFE";

    private final BootstrapDataService bootstrapDataService;
    private final MaintenanceResetProperties properties;
    private final ConfigurableApplicationContext applicationContext;

    MaintenanceResetRunner(
            BootstrapDataService bootstrapDataService,
            MaintenanceResetProperties properties,
            ConfigurableApplicationContext applicationContext) {
        this.bootstrapDataService = bootstrapDataService;
        this.properties = properties;
        this.applicationContext = applicationContext;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!REQUIRED_CONFIRMATION.equals(properties.confirmation())) {
            throw new IllegalStateException(
                    "Maintenance reset is enabled but confirmation does not match the required token.");
        }

        log.warn("Running maintenance reset. This will wipe all application data and recreate the minimal bootstrap.");
        bootstrapDataService.reset();
        log.warn("Maintenance reset completed.");

        if (properties.exitAfterReset()) {
            int exitCode = SpringApplication.exit(applicationContext, () -> 0);
            System.exit(exitCode);
        }
    }
}
