package br.com.doalocal.dao;

import br.com.doalocal.config.ConnectionFactory;
import br.com.doalocal.exception.DataAccessException;
import br.com.doalocal.exception.DoacaoNotFoundException;
import br.com.doalocal.exception.FamiliaNotFoundException;
import br.com.doalocal.exception.InsufficientDonationQuantityException;
import br.com.doalocal.model.DoacaoDisponivel;
import br.com.doalocal.model.Entrega;
import br.com.doalocal.model.EntregaResumo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Persistência transacional e consultas do histórico de entregas. */
public final class EntregaDao {

    private static final String LOCK_DONATION_SQL = """
            UPDATE doacao
            SET quantidade = quantidade
            WHERE id_doacao = ?
            """;

    private static final String FAMILY_EXISTS_SQL =
            "SELECT 1 FROM familia WHERE id_familia = ?";

    private static final String AVAILABLE_QUANTITY_SQL = """
            SELECT d.quantidade - COALESCE(SUM(e.quantidade_entregue), 0) AS disponivel
            FROM doacao d
            LEFT JOIN entrega e ON e.id_doacao = d.id_doacao
            WHERE d.id_doacao = ?
            GROUP BY d.id_doacao, d.quantidade
            """;

    private static final String INSERT_SQL = """
            INSERT INTO entrega (
                id_familia, id_doacao, quantidade_entregue, data_entrega, observacao
            ) VALUES (?, ?, ?, ?, ?)
            RETURNING id_entrega
            """;

    private static final String FIND_ALL_SUMMARY_SQL = """
            SELECT
                e.id_entrega,
                f.id_familia,
                f.nome_responsavel,
                f.cpf_responsavel,
                d.id_doacao,
                d.tipo,
                d.descricao,
                e.quantidade_entregue,
                e.data_entrega,
                e.observacao
            FROM entrega e
            INNER JOIN familia f ON f.id_familia = e.id_familia
            INNER JOIN doacao d ON d.id_doacao = e.id_doacao
            ORDER BY e.data_entrega DESC, e.id_entrega DESC
            """;

    private static final String FIND_AVAILABILITY_SQL = """
            SELECT
                d.id_doacao,
                d.tipo,
                d.descricao,
                d.quantidade AS recebida,
                COALESCE(SUM(e.quantidade_entregue), 0) AS entregue,
                d.quantidade - COALESCE(SUM(e.quantidade_entregue), 0) AS disponivel
            FROM doacao d
            LEFT JOIN entrega e ON e.id_doacao = d.id_doacao
            GROUP BY d.id_doacao, d.tipo, d.descricao, d.quantidade
            ORDER BY d.data_doacao DESC, d.id_doacao DESC
            """;

    public Entrega insertWithAvailabilityCheck(
            long familiaId,
            long doacaoId,
            int quantidadeEntregue,
            LocalDate dataEntrega,
            String observacao
    ) {
        try (Connection connection = ConnectionFactory.openConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                lockAndVerifyDonation(connection, doacaoId);
                verifyFamily(connection, familiaId);

                int available = findAvailableQuantity(connection, doacaoId);
                if (quantidadeEntregue > available) {
                    throw new InsufficientDonationQuantityException(available);
                }

                long id = insert(
                        connection, familiaId, doacaoId, quantidadeEntregue, dataEntrega, observacao
                );
                connection.commit();
                connection.setAutoCommit(previousAutoCommit);
                return new Entrega(
                        id, familiaId, doacaoId, quantidadeEntregue, dataEntrega, observacao
                );
            } catch (SQLException | RuntimeException exception) {
                rollbackAndRestore(connection, previousAutoCommit, exception);
                throw exception;
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Não foi possível registrar a entrega.", exception);
        }
    }

    public List<EntregaResumo> findAllSummaries() {
        List<EntregaResumo> summaries = new ArrayList<>();
        try (Connection connection = ConnectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_ALL_SUMMARY_SQL);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                summaries.add(mapSummary(result));
            }
            return summaries;
        } catch (SQLException exception) {
            throw new DataAccessException("Não foi possível consultar o histórico de entregas.", exception);
        }
    }

    public List<DoacaoDisponivel> findDonationAvailability() {
        List<DoacaoDisponivel> availability = new ArrayList<>();
        try (Connection connection = ConnectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_AVAILABILITY_SQL);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                availability.add(new DoacaoDisponivel(
                        result.getLong("id_doacao"),
                        result.getString("tipo"),
                        result.getString("descricao"),
                        result.getInt("recebida"),
                        result.getInt("entregue"),
                        result.getInt("disponivel")
                ));
            }
            return availability;
        } catch (SQLException exception) {
            throw new DataAccessException("Não foi possível calcular as quantidades disponíveis.", exception);
        }
    }

    private static void lockAndVerifyDonation(Connection connection, long doacaoId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(LOCK_DONATION_SQL)) {
            statement.setLong(1, doacaoId);
            if (statement.executeUpdate() != 1) {
                throw new DoacaoNotFoundException();
            }
        }
    }

    private static void verifyFamily(Connection connection, long familiaId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(FAMILY_EXISTS_SQL)) {
            statement.setLong(1, familiaId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new FamiliaNotFoundException();
                }
            }
        }
    }

    private static int findAvailableQuantity(Connection connection, long doacaoId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(AVAILABLE_QUANTITY_SQL)) {
            statement.setLong(1, doacaoId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new DoacaoNotFoundException();
                }
                return result.getInt("disponivel");
            }
        }
    }

    private static long insert(
            Connection connection,
            long familiaId,
            long doacaoId,
            int quantidadeEntregue,
            LocalDate dataEntrega,
            String observacao
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
            statement.setLong(1, familiaId);
            statement.setLong(2, doacaoId);
            statement.setInt(3, quantidadeEntregue);
            statement.setString(4, dataEntrega.toString());
            statement.setString(5, observacao);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new SQLException("O banco não retornou a entrega criada.");
                }
                return result.getLong("id_entrega");
            }
        }
    }

    private static EntregaResumo mapSummary(ResultSet result) throws SQLException {
        return new EntregaResumo(
                result.getLong("id_entrega"),
                result.getLong("id_familia"),
                result.getString("nome_responsavel"),
                result.getString("cpf_responsavel"),
                result.getLong("id_doacao"),
                result.getString("tipo"),
                result.getString("descricao"),
                result.getInt("quantidade_entregue"),
                LocalDate.parse(result.getString("data_entrega")),
                result.getString("observacao")
        );
    }

    private static void rollbackAndRestore(
            Connection connection,
            boolean previousAutoCommit,
            Throwable primaryException
    ) {
        try {
            connection.rollback();
        } catch (SQLException rollbackException) {
            primaryException.addSuppressed(rollbackException);
        }
        try {
            connection.setAutoCommit(previousAutoCommit);
        } catch (SQLException restoreException) {
            primaryException.addSuppressed(restoreException);
        }
    }
}
