package br.com.doalocal.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.stream.Collectors;

/** Inicializa de forma idempotente o schema necessário pela aplicação. */
public final class DatabaseInitializer {

    private static final String SCHEMA_RESOURCE = "/db/schema.sql";

    private DatabaseInitializer() {
    }

    public static void initialize() throws SQLException {
        String schema = readSchema();

        try (Connection connection = ConnectionFactory.openConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                executeStatements(connection, schema);
                migrateDonationDescription(connection);
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        }
    }

    private static String readSchema() throws SQLException {
        try (InputStream input = DatabaseInitializer.class.getResourceAsStream(SCHEMA_RESOURCE)) {
            if (input == null) {
                throw new SQLException("O recurso de inicialização do banco não foi encontrado.");
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new SQLException("Não foi possível ler o recurso de inicialização do banco.", exception);
        }
    }

    private static void executeStatements(Connection connection, String schema) throws SQLException {
        String scriptWithoutLineComments = Arrays.stream(schema.split("\\R"))
                .filter(line -> !line.stripLeading().startsWith("--"))
                .collect(Collectors.joining(System.lineSeparator()));

        for (String sql : scriptWithoutLineComments.split(";")) {
            if (!sql.isBlank()) {
                try (Statement statement = connection.createStatement()) {
                    statement.execute(sql);
                }
            }
        }
    }

    /** Compatibilidade com bancos criados antes da inclusão da descrição da doação. */
    private static void migrateDonationDescription(Connection connection) throws SQLException {
        boolean descriptionExists = false;
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("PRAGMA table_info(doacao)")) {
            while (result.next()) {
                if ("descricao".equalsIgnoreCase(result.getString("name"))) {
                    descriptionExists = true;
                    break;
                }
            }
        }

        if (!descriptionExists) {
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate(
                        "ALTER TABLE doacao ADD COLUMN descricao TEXT NOT NULL DEFAULT ''"
                );
            }
        }
    }
}
