package br.com.doalocal.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/** Ponto único para abertura e configuração das conexões SQLite. */
public final class ConnectionFactory {

    private static final int BUSY_TIMEOUT_MILLISECONDS = 5_000;

    private ConnectionFactory() {
    }

    public static Connection openConnection() throws SQLException {
        Path databasePath = DatabaseConfig.databasePath();
        createParentDirectory(databasePath);

        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
        try {
            configure(connection);
            return connection;
        } catch (SQLException exception) {
            connection.close();
            throw exception;
        }
    }

    private static void createParentDirectory(Path databasePath) throws SQLException {
        Path parent = databasePath.getParent();
        if (parent == null) {
            return;
        }

        try {
            Files.createDirectories(parent);
        } catch (IOException exception) {
            throw new SQLException("Não foi possível preparar o diretório do banco de dados.", exception);
        }
    }

    private static void configure(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA busy_timeout = " + BUSY_TIMEOUT_MILLISECONDS);
        }

        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("PRAGMA foreign_keys")) {
            if (!result.next() || result.getInt(1) != 1) {
                throw new SQLException("O SQLite não habilitou a integridade referencial.");
            }
        }
    }
}
