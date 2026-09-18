package br.com.doalocal.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DatabaseFoundationTest {

    @TempDir
    Path temporaryDirectory;

    private String previousDatabasePath;

    @BeforeEach
    void configureTemporaryDatabase() {
        previousDatabasePath = System.getProperty(DatabaseConfig.PATH_PROPERTY);
        System.setProperty(
                DatabaseConfig.PATH_PROPERTY,
                temporaryDirectory.resolve("foundation-test.db").toString()
        );
    }

    @AfterEach
    void restoreDatabaseConfiguration() {
        if (previousDatabasePath == null) {
            System.clearProperty(DatabaseConfig.PATH_PROPERTY);
        } else {
            System.setProperty(DatabaseConfig.PATH_PROPERTY, previousDatabasePath);
        }
    }

    @Test
    void initializesExpectedTablesAndEnablesForeignKeys() throws SQLException {
        DatabaseInitializer.initialize();
        DatabaseInitializer.initialize();

        try (Connection connection = ConnectionFactory.openConnection()) {
            assertEquals(1, pragmaForeignKeys(connection));
            assertEquals(
                    Set.of("administrador", "doacao", "entrega", "familia"),
                    applicationTables(connection)
            );
        }
    }

    @Test
    void rejectsDeliveryWithoutExistingFamilyAndDonation() throws SQLException {
        DatabaseInitializer.initialize();

        try (Connection connection = ConnectionFactory.openConnection();
             Statement statement = connection.createStatement()) {
            assertThrows(SQLException.class, () -> statement.executeUpdate("""
                    INSERT INTO entrega
                        (id_familia, id_doacao, quantidade_entregue, data_entrega)
                    VALUES (999, 999, 1, '2026-09-18')
                    """));
        }
    }

    private static int pragmaForeignKeys(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("PRAGMA foreign_keys")) {
            result.next();
            return result.getInt(1);
        }
    }

    private static Set<String> applicationTables(Connection connection) throws SQLException {
        Set<String> tables = new TreeSet<>();
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("""
                     SELECT name
                     FROM sqlite_master
                     WHERE type = 'table'
                       AND name NOT LIKE 'sqlite_%'
                     ORDER BY name
                     """)) {
            while (result.next()) {
                tables.add(result.getString("name"));
            }
        }
        return tables;
    }
}
