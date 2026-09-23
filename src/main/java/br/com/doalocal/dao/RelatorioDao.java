package br.com.doalocal.dao;

import br.com.doalocal.config.ConnectionFactory;
import br.com.doalocal.exception.DataAccessException;
import br.com.doalocal.model.RelatorioEntregaItem;
import br.com.doalocal.model.RelatorioFiltro;
import br.com.doalocal.model.RelatorioOpcoes;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Consultas especializadas e parametrizadas da área de Relatórios. */
public final class RelatorioDao {

    private static final String DELIVERY_SELECT = """
            SELECT
                e.id_entrega,
                e.data_entrega,
                e.quantidade_entregue,
                e.observacao,
                f.id_familia,
                f.nome_responsavel,
                f.cpf_responsavel,
                f.quantidade_membros,
                d.id_doacao,
                d.tipo,
                d.descricao
            FROM entrega e
            INNER JOIN familia f ON f.id_familia = e.id_familia
            INNER JOIN doacao d ON d.id_doacao = e.id_doacao
            WHERE 1 = 1
            """;

    public List<RelatorioEntregaItem> findDeliveries(RelatorioFiltro filtro) {
        StringBuilder sql = new StringBuilder(DELIVERY_SELECT);
        List<Object> parameters = new ArrayList<>();

        if (filtro.dataInicial() != null) {
            sql.append(" AND e.data_entrega >= ?");
            parameters.add(filtro.dataInicial().toString());
        }
        if (filtro.dataFinal() != null) {
            sql.append(" AND e.data_entrega <= ?");
            parameters.add(filtro.dataFinal().toString());
        }
        if (filtro.familiaId() != null) {
            sql.append(" AND e.id_familia = ?");
            parameters.add(filtro.familiaId());
        }
        if (filtro.doacaoId() != null) {
            sql.append(" AND e.id_doacao = ?");
            parameters.add(filtro.doacaoId());
        }
        sql.append(" ORDER BY e.data_entrega ASC, e.id_entrega ASC");

        List<RelatorioEntregaItem> items = new ArrayList<>();
        try (Connection connection = ConnectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            for (int index = 0; index < parameters.size(); index++) {
                statement.setObject(index + 1, parameters.get(index));
            }
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    items.add(mapItem(result));
                }
            }
            return items;
        } catch (SQLException exception) {
            throw new DataAccessException("Não foi possível consultar os dados do relatório.", exception);
        }
    }

    public RelatorioOpcoes findOptions() {
        String familySql = """
                SELECT id_familia, nome_responsavel
                FROM familia
                ORDER BY nome_responsavel COLLATE NOCASE, id_familia
                """;
        String donationSql = """
                SELECT id_doacao, tipo, descricao
                FROM doacao
                ORDER BY data_doacao DESC, id_doacao DESC
                """;
        List<RelatorioOpcoes.FamiliaOpcao> families = new ArrayList<>();
        List<RelatorioOpcoes.DoacaoOpcao> donations = new ArrayList<>();

        try (Connection connection = ConnectionFactory.openConnection();
             PreparedStatement familyStatement = connection.prepareStatement(familySql);
             ResultSet familyResult = familyStatement.executeQuery()) {
            while (familyResult.next()) {
                families.add(new RelatorioOpcoes.FamiliaOpcao(
                        familyResult.getLong("id_familia"),
                        familyResult.getString("nome_responsavel")
                ));
            }
            try (PreparedStatement donationStatement = connection.prepareStatement(donationSql);
                 ResultSet donationResult = donationStatement.executeQuery()) {
                while (donationResult.next()) {
                    donations.add(new RelatorioOpcoes.DoacaoOpcao(
                            donationResult.getLong("id_doacao"),
                            donationResult.getString("tipo"),
                            donationResult.getString("descricao")
                    ));
                }
            }
            return new RelatorioOpcoes(List.copyOf(families), List.copyOf(donations));
        } catch (SQLException exception) {
            throw new DataAccessException("Não foi possível carregar os filtros do relatório.", exception);
        }
    }

    public int countFamilies() {
        return count("SELECT COUNT(*) FROM familia");
    }

    public int countDonations() {
        return count("SELECT COUNT(*) FROM doacao");
    }

    public boolean familyExists(long id) {
        return exists("SELECT 1 FROM familia WHERE id_familia = ?", id);
    }

    public boolean donationExists(long id) {
        return exists("SELECT 1 FROM doacao WHERE id_doacao = ?", id);
    }

    private static int count(String sql) {
        try (Connection connection = ConnectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            return result.next() ? result.getInt(1) : 0;
        } catch (SQLException exception) {
            throw new DataAccessException("Não foi possível calcular os totais do relatório.", exception);
        }
    }

    private static boolean exists(String sql, long id) {
        try (Connection connection = ConnectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Não foi possível validar o filtro do relatório.", exception);
        }
    }

    private static RelatorioEntregaItem mapItem(ResultSet result) throws SQLException {
        return new RelatorioEntregaItem(
                result.getLong("id_entrega"),
                LocalDate.parse(result.getString("data_entrega")),
                result.getLong("id_familia"),
                result.getString("nome_responsavel"),
                result.getString("cpf_responsavel"),
                result.getInt("quantidade_membros"),
                result.getLong("id_doacao"),
                result.getString("tipo"),
                result.getString("descricao"),
                result.getInt("quantidade_entregue"),
                result.getString("observacao")
        );
    }
}
