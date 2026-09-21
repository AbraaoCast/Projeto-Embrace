package br.com.doalocal.dao;

import br.com.doalocal.config.ConnectionFactory;
import br.com.doalocal.exception.DataAccessException;
import br.com.doalocal.exception.DuplicateCpfException;
import br.com.doalocal.exception.RelatedRecordsException;
import br.com.doalocal.model.Familia;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Acesso centralizado à tabela de famílias. */
public final class FamiliaDao {

    private static final DateTimeFormatter SQLITE_DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String COLUMNS = """
            id_familia, cpf_responsavel, nome_responsavel, quantidade_membros,
            telefone, logradouro, numero_residencia, bairro, cidade, data_cadastro
            """;

    private static final String INSERT_SQL = """
            INSERT INTO familia (
                cpf_responsavel, nome_responsavel, quantidade_membros, telefone,
                logradouro, numero_residencia, bairro, cidade, data_cadastro
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING
            """ + COLUMNS;

    private static final String FIND_BY_ID_SQL =
            "SELECT " + COLUMNS + " FROM familia WHERE id_familia = ?";

    private static final String FIND_BY_CPF_SQL =
            "SELECT " + COLUMNS + " FROM familia WHERE cpf_responsavel = ?";

    private static final String FIND_ALL_SQL =
            "SELECT " + COLUMNS + " FROM familia ORDER BY nome_responsavel COLLATE NOCASE, id_familia";

    private static final String UPDATE_SQL = """
            UPDATE familia
            SET cpf_responsavel = ?, nome_responsavel = ?, quantidade_membros = ?,
                telefone = ?, logradouro = ?, numero_residencia = ?, bairro = ?, cidade = ?
            WHERE id_familia = ?
            """;

    private static final String DELETE_SQL = "DELETE FROM familia WHERE id_familia = ?";

    public Familia insert(
            String cpf,
            String nome,
            int quantidadeMembros,
            String telefone,
            String logradouro,
            String numeroResidencia,
            String bairro,
            String cidade,
            LocalDateTime dataCadastro
    ) {
        try (Connection connection = ConnectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
            bindFields(statement, cpf, nome, quantidadeMembros, telefone, logradouro,
                    numeroResidencia, bairro, cidade);
            statement.setString(9, SQLITE_DATE_TIME.format(dataCadastro));
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new DataAccessException("O banco não retornou a família criada.", null);
                }
                return map(result);
            }
        } catch (SQLException exception) {
            throw translate("Não foi possível cadastrar a família.", exception);
        }
    }

    public Optional<Familia> findById(long id) {
        return findOne(FIND_BY_ID_SQL, id, "Não foi possível consultar a família.");
    }

    public Optional<Familia> findByCpf(String cpf) {
        return findOne(FIND_BY_CPF_SQL, cpf, "Não foi possível consultar o CPF da família.");
    }

    public List<Familia> findAll() {
        List<Familia> familias = new ArrayList<>();
        try (Connection connection = ConnectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_ALL_SQL);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                familias.add(map(result));
            }
            return familias;
        } catch (SQLException exception) {
            throw new DataAccessException("Não foi possível listar as famílias.", exception);
        }
    }

    public boolean update(
            long id,
            String cpf,
            String nome,
            int quantidadeMembros,
            String telefone,
            String logradouro,
            String numeroResidencia,
            String bairro,
            String cidade
    ) {
        try (Connection connection = ConnectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {
            bindFields(statement, cpf, nome, quantidadeMembros, telefone, logradouro,
                    numeroResidencia, bairro, cidade);
            statement.setLong(9, id);
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) {
            throw translate("Não foi possível atualizar a família.", exception);
        }
    }

    public boolean delete(long id) {
        try (Connection connection = ConnectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(DELETE_SQL)) {
            statement.setLong(1, id);
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) {
            throw translate("Não foi possível excluir a família.", exception);
        }
    }

    private Optional<Familia> findOne(String sql, Object value, String failureMessage) {
        try (Connection connection = ConnectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, value);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(map(result)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new DataAccessException(failureMessage, exception);
        }
    }

    private static void bindFields(
            PreparedStatement statement,
            String cpf,
            String nome,
            int quantidadeMembros,
            String telefone,
            String logradouro,
            String numeroResidencia,
            String bairro,
            String cidade
    ) throws SQLException {
        statement.setString(1, cpf);
        statement.setString(2, nome);
        statement.setInt(3, quantidadeMembros);
        statement.setString(4, telefone);
        statement.setString(5, logradouro);
        statement.setString(6, numeroResidencia);
        statement.setString(7, bairro);
        statement.setString(8, cidade);
    }

    private static Familia map(ResultSet result) throws SQLException {
        return new Familia(
                result.getLong("id_familia"),
                result.getString("cpf_responsavel"),
                result.getString("nome_responsavel"),
                result.getInt("quantidade_membros"),
                result.getString("telefone"),
                result.getString("logradouro"),
                result.getString("numero_residencia"),
                result.getString("bairro"),
                result.getString("cidade"),
                LocalDateTime.parse(result.getString("data_cadastro"), SQLITE_DATE_TIME)
        );
    }

    private static RuntimeException translate(String message, SQLException exception) {
        String databaseMessage = exception.getMessage();
        String normalizedMessage = databaseMessage == null
                ? ""
                : databaseMessage.toLowerCase(Locale.ROOT);

        if (exception.getErrorCode() == 19 && normalizedMessage.contains("familia.cpf_responsavel")) {
            return new DuplicateCpfException();
        }
        if (exception.getErrorCode() == 19 && normalizedMessage.contains("foreign key constraint")) {
            return new RelatedRecordsException();
        }
        return new DataAccessException(message, exception);
    }
}
