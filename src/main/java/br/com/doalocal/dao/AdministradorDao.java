package br.com.doalocal.dao;

import br.com.doalocal.config.ConnectionFactory;
import br.com.doalocal.exception.DataAccessException;
import br.com.doalocal.exception.DuplicateEmailException;
import br.com.doalocal.model.Administrador;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Optional;

/** Persistência mínima necessária para cadastro e autenticação do administrador. */
public final class AdministradorDao {

    private static final String INSERT_SQL = """
            INSERT INTO administrador (nome, email, senha_hash)
            VALUES (?, ?, ?)
            RETURNING id_administrador
            """;

    private static final String FIND_BY_EMAIL_SQL = """
            SELECT id_administrador, nome, email, senha_hash
            FROM administrador
            WHERE email = ?
            """;

    public Administrador insert(String nome, String email, String senhaHash) {
        try (Connection connection = ConnectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
            statement.setString(1, nome);
            statement.setString(2, email);
            statement.setString(3, senhaHash);

            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new DataAccessException("O banco não retornou o administrador criado.", null);
                }
                return new Administrador(result.getLong(1), nome, email, senhaHash);
            }
        } catch (SQLException exception) {
            if (isDuplicateEmailViolation(exception)) {
                throw new DuplicateEmailException();
            }
            throw new DataAccessException("Não foi possível cadastrar o administrador.", exception);
        }
    }

    public Optional<Administrador> findByEmail(String email) {
        try (Connection connection = ConnectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_BY_EMAIL_SQL)) {
            statement.setString(1, email);

            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return Optional.empty();
                }
                return Optional.of(map(result));
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Não foi possível consultar o administrador.", exception);
        }
    }

    private static Administrador map(ResultSet result) throws SQLException {
        return new Administrador(
                result.getLong("id_administrador"),
                result.getString("nome"),
                result.getString("email"),
                result.getString("senha_hash")
        );
    }

    private static boolean isDuplicateEmailViolation(SQLException exception) {
        for (SQLException current = exception; current != null; current = current.getNextException()) {
            String message = current.getMessage();
            if (current.getErrorCode() == 19
                    && message != null
                    && message.toLowerCase(Locale.ROOT).contains("administrador.email")) {
                return true;
            }
        }
        return false;
    }
}
