package br.com.doalocal.dao;

import br.com.doalocal.config.ConnectionFactory;
import br.com.doalocal.exception.DataAccessException;
import br.com.doalocal.model.DashboardAtividade;
import br.com.doalocal.model.DashboardResumo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Consultas agregadas e limitadas utilizadas exclusivamente pelo Dashboard. */
public final class DashboardDao {

    private static final String SUMMARY_SQL = """
            SELECT
                (SELECT COUNT(*) FROM familia) AS total_familias,
                (SELECT COUNT(*) FROM doacao) AS total_doacoes,
                (SELECT COUNT(*) FROM entrega) AS total_entregas,
                (SELECT COUNT(DISTINCT id_familia) FROM entrega) AS familias_atendidas
            """;

    private static final String RECENT_ACTIVITIES_SQL = """
            SELECT
                atividade_id,
                tipo_atividade,
                titulo,
                detalhe,
                quantidade,
                data_atividade
            FROM (
                SELECT
                    f.id_familia AS atividade_id,
                    'familia' AS tipo_atividade,
                    f.nome_responsavel AS titulo,
                    NULL AS detalhe,
                    NULL AS quantidade,
                    DATE(f.data_cadastro) AS data_atividade,
                    1 AS prioridade
                FROM familia f

                UNION ALL

                SELECT
                    d.id_doacao AS atividade_id,
                    'doacao' AS tipo_atividade,
                    d.tipo AS titulo,
                    d.descricao AS detalhe,
                    d.quantidade AS quantidade,
                    d.data_doacao AS data_atividade,
                    2 AS prioridade
                FROM doacao d

                UNION ALL

                SELECT
                    e.id_entrega AS atividade_id,
                    'entrega' AS tipo_atividade,
                    f.nome_responsavel AS titulo,
                    d.tipo || CASE
                        WHEN TRIM(d.descricao) = '' THEN ''
                        ELSE ' — ' || d.descricao
                    END AS detalhe,
                    e.quantidade_entregue AS quantidade,
                    e.data_entrega AS data_atividade,
                    3 AS prioridade
                FROM entrega e
                INNER JOIN familia f ON f.id_familia = e.id_familia
                INNER JOIN doacao d ON d.id_doacao = e.id_doacao
            ) atividades
            ORDER BY data_atividade DESC, prioridade DESC, atividade_id DESC
            LIMIT ?
            """;

    public DashboardResumo findSummary() {
        try (Connection connection = ConnectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(SUMMARY_SQL);
             ResultSet result = statement.executeQuery()) {
            if (!result.next()) {
                throw new DataAccessException("O banco não retornou o resumo do Dashboard.", null);
            }
            return new DashboardResumo(
                    result.getInt("total_familias"),
                    result.getInt("total_doacoes"),
                    result.getInt("total_entregas"),
                    result.getInt("familias_atendidas")
            );
        } catch (SQLException exception) {
            throw new DataAccessException("Não foi possível carregar o resumo do Dashboard.", exception);
        }
    }

    public List<DashboardAtividade> findRecentActivities(int limit) {
        List<DashboardAtividade> activities = new ArrayList<>();
        try (Connection connection = ConnectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(RECENT_ACTIVITIES_SQL)) {
            statement.setInt(1, limit);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    activities.add(new DashboardAtividade(
                            result.getLong("atividade_id"),
                            result.getString("tipo_atividade"),
                            result.getString("titulo"),
                            result.getString("detalhe"),
                            nullableInteger(result, "quantidade"),
                            LocalDate.parse(result.getString("data_atividade"))
                    ));
                }
            }
            return activities;
        } catch (SQLException exception) {
            throw new DataAccessException("Não foi possível carregar as atividades recentes.", exception);
        }
    }

    private static Integer nullableInteger(ResultSet result, String column) throws SQLException {
        int value = result.getInt(column);
        return result.wasNull() ? null : value;
    }
}
