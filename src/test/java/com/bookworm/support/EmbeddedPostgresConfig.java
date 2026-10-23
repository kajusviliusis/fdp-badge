package com.bookworm.support;

import java.io.IOException;
import javax.sql.DataSource;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Provides a real PostgreSQL server (embedded binaries, no Docker needed) as the application {@link DataSource}.
 */
@TestConfiguration(proxyBeanMethods = false)
public class EmbeddedPostgresConfig {

    @Bean(destroyMethod = "close")
    EmbeddedPostgres embeddedPostgres() throws IOException {
        return EmbeddedPostgres.start();
    }

    @Bean
    DataSource dataSource(EmbeddedPostgres embeddedPostgres) {
        return embeddedPostgres.getPostgresDatabase();
    }
}
