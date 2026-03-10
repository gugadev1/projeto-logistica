package com.projetologistica.persistence;

public record DatabaseConfig(
        String jdbcUrl,
        String username,
        String password,
        String schema,
        int maxPoolSize
) {
    public static DatabaseConfig fromEnvironment() {
        String jdbcUrl = getenvOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/logistica");
        String username = getenvOrDefault("DB_USER", "logistica");
        String password = getenvOrDefault("DB_PASSWORD", "logistica");
        String schema = getenvOrDefault("DB_SCHEMA", "public");
        int maxPoolSize = Integer.parseInt(getenvOrDefault("DB_POOL_SIZE", "10"));

        return new DatabaseConfig(jdbcUrl, username, password, schema, maxPoolSize);
    }

    private static String getenvOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value;
    }
}
