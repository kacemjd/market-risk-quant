package com.kacemrisk.market.infrastructure.config;

import lombok.RequiredArgsConstructor;
import org.apache.spark.sql.SparkSession;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for the Spark session. The session is a singleton managed by the Spring
 * context.
 *
 * <p>All tuning knobs (master URL, serializer, Kryo class list, …) are resolved from {@link
 * SparkProperties}, which is bound to the {@code spark.*} namespace in {@code application.yml}. No
 * Spark setting should be hardcoded here.
 *
 * <p>Kryo class registration eliminates the full classname overhead embedded in every serialised
 * record when {@code registrationRequired=false} — measurable at large row counts. Registered types
 * get compact integer IDs in the byte stream; unregistered ad-hoc types (closures, etc.) fall back
 * gracefully because {@code registrationRequired=false}.
 */
@Configuration
@RequiredArgsConstructor
public class SparkConfig {

    private final SparkProperties sparkProperties;

    @Bean
    public SparkSession sparkSession() {
        SparkProperties.Serializer serializer = sparkProperties.getSerializer();
        SparkProperties.Serializer.Kryo kryo = serializer.getKryo();
        return SparkSession.builder()
                .appName(sparkProperties.getAppName())
                .master(sparkProperties.getMaster())
                .config("spark.serializer", serializer.getType())
                .config(
                        "spark.kryo.registrationRequired",
                        String.valueOf(kryo.isRegistrationRequired()))
                .config(
                        "spark.kryo.classesToRegister",
                        String.join(",", kryo.getClassesToRegister()))
                .getOrCreate();
    }
}
