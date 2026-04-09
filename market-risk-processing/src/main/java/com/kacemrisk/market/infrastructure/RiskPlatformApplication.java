package com.kacemrisk.market.infrastructure;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.kacemrisk.market")
@EnableScheduling
public class RiskPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(RiskPlatformApplication.class, args);
    }
}
