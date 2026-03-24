package com.speedline.user.config;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Appelle repair() avant migrate() afin d'effacer les entrées FAILED dans
 * flyway_schema_history avant de relancer les migrations.
 * Cela permet de rejouer idempotently les migrations dont le SQL a été corrigé.
 */
@Configuration
@Slf4j
public class FlywayConfig {

    @Bean
    public FlywayMigrationStrategy repairAndMigrate() {
        return flyway -> {
            log.info("Flyway repair: nettoyage des migrations FAILED...");
            flyway.repair();
            log.info("Flyway migrate: démarrage des migrations...");
            flyway.migrate();
        };
    }
}
