package com.kacemrisk.market.infrastructure.adapter.out.persistence;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@Profile("questdb")
@RequiredArgsConstructor
public class QuestDbMigrationRunner {

    private final JdbcTemplate jdbc;

    @PostConstruct
    public void migrate() throws IOException {
        bootstrap();
        Set<String> applied = appliedVersions();

        Resource[] scripts = new PathMatchingResourcePatternResolver()
                .getResources("classpath:questdb/migration/V*.sql");

        Arrays.stream(scripts)
                .sorted(Comparator.comparing(Resource::getFilename))
                .forEach(r -> apply(r, applied));

        log.info("QuestDB migrations complete");
    }

    private void bootstrap() {
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS schema_version (
                    version     SYMBOL,
                    script_name SYMBOL,
                    applied_at  TIMESTAMP
                ) TIMESTAMP(applied_at) PARTITION BY YEAR WAL;
                """);
    }

    private Set<String> appliedVersions() {
        return jdbc.queryForList("SELECT version FROM schema_version", String.class)
                .stream().collect(Collectors.toSet());
    }

    private void apply(Resource script, Set<String> applied) {
        String filename = script.getFilename();
        String version  = filename.substring(0, filename.indexOf("__"));
        if (applied.contains(version)) {
            log.debug("Migration {} already applied — skipping", filename);
            return;
        }
        try {
            String sql = script.getContentAsString(StandardCharsets.UTF_8);
            for (String statement : sql.split(";")) {
                String trimmed = statement.strip();
                if (!trimmed.isEmpty()) jdbc.execute(trimmed);
            }
            jdbc.update("INSERT INTO schema_version (version, script_name, applied_at) VALUES (?, ?, systimestamp())",
                    version, filename);
            log.info("Applied migration {}", filename);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read migration " + filename, e);
        }
    }
}

