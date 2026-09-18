package br.com.doalocal.config;

import java.nio.file.Path;

/** Resolve a localização externa do banco sem depender de um computador específico. */
public final class DatabaseConfig {

    public static final String PATH_PROPERTY = "doalocal.database.path";
    public static final String PATH_ENVIRONMENT_VARIABLE = "DOALOCAL_DATABASE_PATH";
    private static final Path DEFAULT_PATH = Path.of("data", "doalocal.db");

    private DatabaseConfig() {
    }

    public static Path databasePath() {
        String configuredPath = System.getProperty(PATH_PROPERTY);
        if (configuredPath == null || configuredPath.isBlank()) {
            configuredPath = System.getenv(PATH_ENVIRONMENT_VARIABLE);
        }

        Path path = configuredPath == null || configuredPath.isBlank()
                ? DEFAULT_PATH
                : Path.of(configuredPath);

        return path.toAbsolutePath().normalize();
    }
}
