package br.com.doalocal.dao;

import br.com.doalocal.config.ConnectionFactory;
import br.com.doalocal.exception.DataAccessException;
import br.com.doalocal.exception.RelatedDonationRecordsException;
import br.com.doalocal.model.Doacao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Acesso centralizado à tabela de doações. */
public final class DoacaoDao {

    private static final String COLUMNS =
            "id_doacao, tipo, descricao, quantidade, data_doacao, observacao";

    private static final String INSERT_SQL = """
            INSERT INTO doacao (tipo, descricao, quantidade, data_doacao, observacao)
            VALUES (?, ?, ?, ?, ?)
            RETURNING
            """ + COLUMNS;

    private static final String FIND_BY_ID_SQL =
            "SELECT " + COLUMNS + " FROM doacao WHERE id_doacao = ?";

    private static final String FIND_ALL_SQL =
            "SELECT " + COLUMNS + " FROM doacao ORDER BY data_doacao DESC, id_doacao DESC";

    private static final String UPDATE_SQL = """
            UPDATE doacao
            SET tipo = ?, descricao = ?, quantidade = ?, data_doacao = ?, observacao = ?
            WHERE id_doacao = ?
            """;

    private static final String DELETE_SQL = "DELETE FROM doacao WHERE id_doacao = ?";

    public Doacao insert(
            String tipo,
            String descricao,
            int quantidade,
            LocalDate dataDoacao,
            String observacao
    ) {
        try (Connection connection = ConnectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
            bindFields(statement, tipo, descricao, quantidade, dataDoacao, observacao);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new DataAccessException("O banco não retornou a doação criada.", null);
                }
                return map(result);
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Não foi possível cadastrar a doação.", exception);
        }
    }

    public Optional<Doacao> findById(long id) {
        try (Connection connection = ConnectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_BY_ID_SQL)) {
            statement.setLong(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(map(result)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Não foi possível consultar a doação.", exception);
        }
    }

    public List<Doacao> findAll() {
        List<Doacao> doacoes = new ArrayList<>();
        try (Connection connection = ConnectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_ALL_SQL);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                doacoes.add(map(result));
            }
            return doacoes;
        } catch (SQLException exception) {
            throw new DataAccessException("Não foi possível listar as doações.", exception);
        }
    }

    public boolean update(
            long id,
            String tipo,
            String descricao,
            int quantidade,
            LocalDate dataDoacao,
            String observacao
    ) {
        try (Connection connection = ConnectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {
            bindFields(statement, tipo, descricao, quantidade, dataDoacao, observacao);
            statement.setLong(6, id);
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) {
            throw new DataAccessException("Não foi possível atualizar a doação.", exception);
        }
    }

    public boolean delete(long id) {
        try (Connection connection = ConnectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(DELETE_SQL)) {
            statement.setLong(1, id);
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) {
            if (isForeignKeyViolation(exception)) {
                throw new RelatedDonationRecordsException();
            }
            throw new DataAccessException("Não foi possível excluir a doação.", exception);
        }
    }

    private static void bindFields(
            PreparedStatement statement,
            String tipo,
            String descricao,
            int quantidade,
            LocalDate dataDoacao,
            String observacao
    ) throws SQLException {
        statement.setString(1, tipo);
        statement.setString(2, descricao);
        statement.setInt(3, quantidade);
        statement.setString(4, dataDoacao.toString());
        statement.setString(5, observacao);
    }

    private static Doacao map(ResultSet result) throws SQLException {
        return new Doacao(
                result.getLong("id_doacao"),
                result.getString("tipo"),
                result.getString("descricao"),
                result.getInt("quantidade"),
                LocalDate.parse(result.getString("data_doacao")),
                result.getString("observacao")
        );
    }

    private static boolean isForeignKeyViolation(SQLException exception) {
        for (SQLException current = exception; current != null; current = current.getNextException()) {
            String message = current.getMessage();
            if (current.getErrorCode() == 19
                    && message != null
                    && message.toLowerCase(Locale.ROOT).contains("foreign key constraint")) {
                return true;
            }
        }
        return false;
    }
}
