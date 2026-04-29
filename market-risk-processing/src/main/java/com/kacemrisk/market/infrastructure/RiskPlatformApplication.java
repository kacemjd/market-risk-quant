package com.kacemrisk.market.infrastructure;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// DataSource auto-configuration is excluded via spring.autoconfigure.exclude in
// application.yml because QuestDB is optional (questdb profile only).
// See DataSourceConfig for the profile-scoped DataSource bean.
@SpringBootApplication(scanBasePackages = "com.kacemrisk.market")
@EnableScheduling
public class RiskPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(RiskPlatformApplication.class, args);
    }
}
