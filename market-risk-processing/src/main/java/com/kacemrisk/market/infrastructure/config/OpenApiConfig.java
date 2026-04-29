package com.kacemrisk.market.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Market Risk — Quantitative Risk Engine")
                        .description("""
                                REST API for the Market Risk Quantitative Engine.
                                
                                Supports:
                                - **Monte Carlo VaR** — path simulation via Apache Spark
                                - **Parametric VaR** — variance-covariance approach
                                
                                Trigger a scenario by `POST /scenarios/run` with a portfolio CSV path,
                                a prices directory, an as-of date, confidence level and number of paths.
                                """)
                        .version("1.0-SNAPSHOT")
                        .contact(new Contact()
                                .name("KacemRisk")
                                .email("risk@kacemrisk.com")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local development")
                ));
    }
}

