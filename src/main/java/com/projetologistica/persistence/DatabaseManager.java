package com.projetologistica.persistence;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;

import javax.sql.DataSource;

public final class DatabaseManager {
    private static HikariDataSource dataSource;

    private DatabaseManager() {
    }

    public static synchronized void initialize() {
        if (dataSource != null) {
            return;
        }

        DatabaseConfig dbConfig = DatabaseConfig.fromEnvironment();
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(dbConfig.jdbcUrl());
        hikariConfig.setUsername(dbConfig.username());
        hikariConfig.setPassword(dbConfig.password());
        hikariConfig.setMaximumPoolSize(dbConfig.maxPoolSize());
        hikariConfig.setMinimumIdle(1);
        hikariConfig.setPoolName("logistica-pool");
        hikariConfig.setConnectionTimeout(10_000);
        hikariConfig.setIdleTimeout(60_000);
        hikariConfig.setMaxLifetime(300_000);

        dataSource = new HikariDataSource(hikariConfig);

        Flyway.configure()
                .dataSource(dataSource)
                .schemas(dbConfig.schema())
                .locations("classpath:db/migration")
                .load()
                .migrate();
    }

    public static DataSource getDataSource() {
        if (dataSource == null) {
            throw new IllegalStateException("DatabaseManager nao inicializado.");
        }
        return dataSource;
    }
}
