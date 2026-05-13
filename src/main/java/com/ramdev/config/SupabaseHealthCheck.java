package com.ramdev.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Runs at startup (before DataSeeder) to verify the Supabase connection.
 * Prints a clear success or failure message rather than a cryptic stack trace.
 *
 * Order(1) ensures this runs BEFORE DataSeeder (which is Order(2) by default).
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class SupabaseHealthCheck implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        try {
            String version = jdbcTemplate.queryForObject("SELECT version()", String.class);
            log.info("════════════════════════════════════════════════════════");
            log.info("✅  Supabase (PostgreSQL) connected successfully.");
            log.info("    Server: {}", version != null ? version.split(",")[0] : "unknown");
            log.info("════════════════════════════════════════════════════════");
        } catch (Exception e) {
            log.error("════════════════════════════════════════════════════════");
            log.error("❌  SUPABASE CONNECTION FAILED!");
            log.error("    Reason : {}", e.getMessage());
            log.error("    Fix    : Check your password in application.properties");
            log.error("             spring.datasource.password=[YOUR-PASSWORD]");
            log.error("════════════════════════════════════════════════════════");
            // Re-throw so Spring Boot exits cleanly with a non-zero code
            throw new IllegalStateException("Cannot connect to Supabase. See logs above.", e);
        }
    }
}
