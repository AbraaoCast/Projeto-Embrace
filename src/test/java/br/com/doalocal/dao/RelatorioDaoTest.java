package br.com.doalocal.dao;

import br.com.doalocal.config.DatabaseConfig;
import br.com.doalocal.config.DatabaseInitializer;
import br.com.doalocal.model.Doacao;
import br.com.doalocal.model.Familia;
import br.com.doalocal.model.RelatorioEntregaItem;
import br.com.doalocal.model.RelatorioFiltro;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RelatorioDaoTest {

    @TempDir
    Path temporaryDirectory;

    private String previousDatabasePath;
    private RelatorioDao relatorioDao;
    private Familia maria;
    private Familia joao;
    private Doacao cestas;
    private Doacao cobertores;

    @BeforeEach
    void setUp() throws Exception {
        previousDatabasePath = System.getProperty(DatabaseConfig.PATH_PROPERTY);
        System.setProperty(DatabaseConfig.PATH_PROPERTY,
                temporaryDirectory.resolve("relatorio-dao-test.db").toString());
        DatabaseInitializer.initialize();

        FamiliaDao familiaDao = new FamiliaDao();
        DoacaoDao doacaoDao = new DoacaoDao();
        EntregaDao entregaDao = new EntregaDao();
        relatorioDao = new RelatorioDao();

        maria = familiaDao.insert("52998224725", "Maria São Luís", 4, "98999999999",
                "Rua A", "1", "Centro", "São Luís", LocalDateTime.of(2026, 9, 1, 10, 0));
        joao = familiaDao.insert("11144477735", "João da Silva", 2, "11988887777",
                "Rua B", "2", "Centro", "Teresina", LocalDateTime.of(2026, 9, 2, 10, 0));
        cestas = doacaoDao.insert("Alimentos", "Cesta básica", 100,
                LocalDate.of(2026, 9, 1), "Doação solidária");
        cobertores = doacaoDao.insert("Roupas", "Cobertor", 100,
                LocalDate.of(2026, 9, 2), null);

        entregaDao.insertWithAvailabilityCheck(maria.getId(), cestas.getId(), 10,
                LocalDate.of(2026, 9, 10), "Primeira entrega");
        entregaDao.insertWithAvailabilityCheck(joao.getId(), cobertores.getId(), 5,
                LocalDate.of(2026, 9, 15), "Segunda entrega");
        entregaDao.insertWithAvailabilityCheck(maria.getId(), cobertores.getId(), 3,
                LocalDate.of(2026, 9, 20), "Terceira entrega");
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
    void joinsAndOrdersAllReportDataChronologically() {
        List<RelatorioEntregaItem> items = relatorioDao.findDeliveries(
                new RelatorioFiltro(null, null, null, null)
        );

        assertEquals(3, items.size());
        assertEquals(LocalDate.of(2026, 9, 10), items.get(0).dataEntrega());
        assertEquals("Maria São Luís", items.get(0).nomeResponsavel());
        assertEquals("Cesta básica", items.get(0).descricaoDoacao());
        assertEquals(10, items.get(0).quantidadeEntregue());
        assertEquals("Terceira entrega", items.get(2).observacao());
    }

    @Test
    void appliesPeriodAndExcludesOutsideRecords() {
        List<RelatorioEntregaItem> items = relatorioDao.findDeliveries(
                new RelatorioFiltro(
                        LocalDate.of(2026, 9, 11), LocalDate.of(2026, 9, 20), null, null
                )
        );
        assertEquals(List.of(5, 3), items.stream().map(RelatorioEntregaItem::quantidadeEntregue).toList());
    }

    @Test
    void combinesFamilyAndDonationFilters() {
        List<RelatorioEntregaItem> items = relatorioDao.findDeliveries(
                new RelatorioFiltro(null, null, maria.getId(), cobertores.getId())
        );
        assertEquals(1, items.size());
        assertEquals(3, items.getFirst().quantidadeEntregue());
    }

    @Test
    void returnsEmptyResultAndRealOptionsAndCounts() {
        assertTrue(relatorioDao.findDeliveries(new RelatorioFiltro(
                LocalDate.of(2027, 1, 1), LocalDate.of(2027, 1, 31), null, null
        )).isEmpty());
        assertEquals(2, relatorioDao.countFamilies());
        assertEquals(2, relatorioDao.countDonations());
        assertEquals(2, relatorioDao.findOptions().familias().size());
        assertEquals(2, relatorioDao.findOptions().doacoes().size());
        assertTrue(relatorioDao.familyExists(joao.getId()));
        assertTrue(relatorioDao.donationExists(cestas.getId()));
    }
}
