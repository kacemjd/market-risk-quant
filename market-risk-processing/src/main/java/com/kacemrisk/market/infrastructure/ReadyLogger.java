package com.kacemrisk.market.infrastructure;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Logs a prominent "ready" banner once the application context is fully started.
 * Activated only under servlet-based profiles (local, docker) where the REST
 * endpoint is actually exposed.
 */
@Slf4j
@Component
@Profile({"local", "docker"})
public class ReadyLogger {

    private final Environment env;

    public ReadyLogger(Environment env) {
        this.env = env;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        String port = env.getProperty("server.port", "8080");
        String profiles = String.join(", ", env.getActiveProfiles());
        log.info("""
                
                ╔══════════════════════════════════════════════════════════════╗
                ║          market-risk-processing  ·  READY                    ║
                ╠══════════════════════════════════════════════════════════════╣
                ║  Profiles  : {}
                ║  Port      : {}
                ╠══════════════════════════════════════════════════════════════╣
                ║  Trigger a VaR run (all fields optional):                   ║
                ║                                                              ║
                ║  curl -s -X POST \\                                           ║
                ║    http://localhost:{}/scenarios/run \\
                ║    -H 'Content-Type: application/json' \\                     ║
                ║    -d '{{}}'                                                  ║
                ║                                                              ║
                ║  Swagger UI → http://localhost:{}/swagger-ui.html
                ╚══════════════════════════════════════════════════════════════╝
                """, profiles, port, port, port, port);
    }
}


