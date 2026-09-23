package br.com.doalocal.util;

import br.com.doalocal.model.DashboardAtividade;
import br.com.doalocal.model.DashboardResultado;
import br.com.doalocal.model.DashboardResumo;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DashboardJsonTest {

    @Test
    void serializesSummaryActivitiesAndBrazilianDate() {
        String json = DashboardJson.result(new DashboardResultado(
                new DashboardResumo(3, 2, 4, 2),
                List.of(new DashboardAtividade(
                        7, "entrega", "Maria", "Alimentos — Cesta", 2,
                        LocalDate.of(2026, 9, 23)
                ))
        ));

        assertTrue(json.contains("\"totalFamilias\":3"));
        assertTrue(json.contains("\"familiasAtendidas\":2"));
        assertTrue(json.contains("\"data\":\"23/09/2026\""));
        assertTrue(json.contains("\"quantidade\":2"));
    }

    @Test
    void escapesUserProvidedContentInsteadOfProducingExecutableMarkup() {
        String json = DashboardJson.result(new DashboardResultado(
                new DashboardResumo(1, 0, 0, 0),
                List.of(new DashboardAtividade(
                        1, "familia", "<script>\"alert(1)\"</script>", null, null,
                        LocalDate.of(2026, 9, 23)
                ))
        ));

        assertTrue(json.contains("<script>\\\"alert(1)\\\"</script>"));
        assertFalse(json.contains("<script>\"alert(1)\"</script>"));
        assertEquals("{\"error\":\"internal\",\"message\":\"Falha \\\"segura\\\"\"}",
                DashboardJson.error("internal", "Falha \"segura\""));
    }
}
