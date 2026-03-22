package com.oryzem.programmanagementsystem.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.oryzem.programmanagementsystem")
@EntityScan(basePackages = "com.oryzem.programmanagementsystem")
@EnableJpaRepositories(basePackages = "com.oryzem.programmanagementsystem")
public class ProgramManagementSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProgramManagementSystemApplication.class, args);
    }

}
