package com.kacemrisk.market.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Typed binding for all {@code spark.*} properties defined in {@code application.yml}.
 * Keeping Spark tuning knobs in YAML means they can be overridden per-environment
 * (e.g. different serializer or Kryo class list) without recompiling.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "spark")
public class SparkProperties {

    /** Spark application name shown in the UI / history server. */
    private String appName;

    /** Spark master URL — {@code local[*]} for in-process, {@code yarn} or {@code spark://host:port} in production. */
    private String master;

    /** Serializer configuration — type and its specific tuning options. */
    private Serializer serializer = new Serializer();

    @Getter
    @Setter
    public static class Serializer {

        /** Fully-qualified serializer class, e.g. {@code org.apache.spark.serializer.KryoSerializer}. */
        private String type;

        /** Kryo-specific options — only relevant when {@code type} is the KryoSerializer. */
        private Kryo kryo = new Kryo();

        @Getter
        @Setter
        public static class Kryo {

            /**
             * When {@code false} unregistered types are serialised by class name (safe fallback).
             * Set to {@code true} in production to enforce strict registration and catch missing entries early.
             */
            private boolean registrationRequired;

            /** Domain / infrastructure types pre-registered with Kryo for compact integer IDs. */
            private List<String> classesToRegister;
        }
    }
}
