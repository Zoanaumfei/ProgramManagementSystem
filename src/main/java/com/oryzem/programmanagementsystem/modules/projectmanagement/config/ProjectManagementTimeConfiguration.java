package com.oryzem.programmanagementsystem.modules.projectmanagement.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProjectManagementTimeConfiguration {

    @Bean
    Clock projectManagementClock() {
        return Clock.systemUTC();
    }
}
