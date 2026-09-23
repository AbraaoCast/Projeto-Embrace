package br.com.doalocal.service;

import br.com.doalocal.config.DatabaseConfig;
import br.com.doalocal.config.DatabaseInitializer;
import br.com.doalocal.dao.DoacaoDao;
import br.com.doalocal.dao.EntregaDao;
import br.com.doalocal.dao.FamiliaDao;
import br.com.doalocal.dao.RelatorioDao;
import br.com.doalocal.exception.ValidationException;
import br.com.doalocal.model.Doacao;
import br.com.doalocal.model.Familia;
import br.com.doalocal.model.RelatorioResultado;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RelatorioServiceTest {

    @TempDir
    Path temporaryDirectory;

    private String previousDatabasePath;
    private RelatorioService service;
    private Familia maria;
    private Familia joao;
    private Doacao cestas;
    private Doacao cobertores;

    @BeforeEach
    void setUp() throws Exception {
        previousDatabasePath = System.getProperty(DatabaseConfig.PATH_PROPERTY);
        System.setProperty(DatabaseConfig.PATH_PROPERTY,
                temporaryDirectory.resolve("relatorio-service-test.db").toString());
        DatabaseInitializer.initialize();
        FamiliaDao familiaDao = new FamiliaDao();
        DoacaoDao doacaoDao = new DoacaoDao();
        EntregaDao entregaDao = new EntregaDao();
        service = new RelatorioService(new RelatorioDao());

        maria = familiaDao.insert("52998224725", "Maria", 4, "98999999999",
                "Rua A", "1", "Centro", "São Luís", LocalDateTime.now().withNano(0));
        joao = familiaDao.insert("11144477735", "João", 2, "11988887777",
                "Rua B", "2", "Centro", "Teresina", LocalDateTime.now().withNano(0));
        cestas = doacaoDao.insert("Alimentos", "Cesta básica", 100,
                LocalDate.of(2026, 9, 1), null);
        cobertores = doacaoDao.insert("Roupas", "Cobertor", 100,
                LocalDate.of(2026, 9, 2), null);
        entregaDao.insertWithAvailabilityCheck(maria.getId(), cestas.getId(), 10,
                LocalDate.of(2026, 9, 10), null);
        entregaDao.insertWithAvailabilityCheck(maria.getId(), cestas.getId(), 5,
                LocalDate.of(2026, 9, 12), null);
        entregaDao.insertWithAvailabilityCheck(joao.getId(), cobertores.getId(), 3,
                LocalDate.of(2026, 9, 20), null);
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
    void generatesUnfilteredSummaryWithDistinctFamiliesAndGroupedQuantities() {
        RelatorioResultado report = service.generate(null, "", null, " ");
        assertEquals(3, report.resumo().totalEntregas());
        assertEquals(2, report.resumo().familiasAtendidas());
        assertEquals(2, report.resumo().totalFamiliasCadastradas());
        assertEquals(2, report.resumo().totalDoacoesCadastradas());
        assertEquals(2, report.quantidadesPorDoacao().size());
        assertEquals(15, report.quantidadesPorDoacao().getFirst().quantidade());
    }

    @Test
    void combinesPeriodFamilyAndDonationFilters() {
        RelatorioResultado report = service.generate(
                "11/09/2026", "30/09/2026",
                String.valueOf(maria.getId()), String.valueOf(cestas.getId())
        );
        assertEquals(1, report.itens().size());
        assertEquals(5, report.itens().getFirst().quantidadeEntregue());
    }

    @Test
    void returnsEmptyReportWithoutInventingResults() {
        RelatorioResultado report = service.generate("01/01/2027", "31/01/2027", "", "");
        assertTrue(report.itens().isEmpty());
        assertEquals(0, report.resumo().totalEntregas());
        assertEquals(0, report.resumo().familiasAtendidas());
    }

    @Test
    void rejectsReversedPeriodAndManipulatedIds() {
        assertCode("invalid_period", () -> service.generate(
                "30/09/2026", "01/09/2026", null, null
        ));
        assertCode("family_not_found", () -> service.generate(null, null, "999", null));
        assertCode("donation_not_found", () -> service.generate(null, null, null, "999"));
        assertCode("invalid_family", () -> service.generate(null, null, "abc", null));
        assertCode("invalid_donation", () -> service.generate(null, null, null, "-1"));
    }

    @ParameterizedTest
    @MethodSource("invalidDates")
    void rejectsInvalidDates(String start, String end, String code) {
        assertCode(code, () -> service.generate(start, end, null, null));
    }

    @Test
    void providesRealFilterOptions() {
        assertEquals(2, service.listOptions().familias().size());
        assertEquals(2, service.listOptions().doacoes().size());
    }

    private static void assertCode(String code, Runnable operation) {
        ValidationException exception = assertThrows(ValidationException.class, operation::run);
        assertEquals(code, exception.getErrorCode());
    }

    private static Stream<Arguments> invalidDates() {
        return Stream.of(
                Arguments.of("31/02/2026", null, "invalid_start_date"),
                Arguments.of(null, "99/99/9999", "invalid_end_date"),
                Arguments.of("2026-09-01", null, "invalid_start_date"),
                Arguments.of(null, "texto", "invalid_end_date")
        );
    }
}
