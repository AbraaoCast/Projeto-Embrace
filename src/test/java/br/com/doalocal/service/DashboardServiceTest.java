package br.com.doalocal.service;

import br.com.doalocal.config.DatabaseConfig;
import br.com.doalocal.config.DatabaseInitializer;
import br.com.doalocal.dao.DashboardDao;
import br.com.doalocal.dao.DoacaoDao;
import br.com.doalocal.dao.EntregaDao;
import br.com.doalocal.dao.FamiliaDao;
import br.com.doalocal.model.DashboardAtividade;
import br.com.doalocal.model.DashboardResultado;
import br.com.doalocal.model.Doacao;
import br.com.doalocal.model.Familia;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DashboardServiceTest {

    @TempDir
    Path temporaryDirectory;

    private String previousDatabasePath;
    private DashboardService dashboardService;
    private FamiliaDao familiaDao;
    private DoacaoDao doacaoDao;
    private EntregaDao entregaDao;

    @BeforeEach
    void setUp() throws Exception {
        previousDatabasePath = System.getProperty(DatabaseConfig.PATH_PROPERTY);
        System.setProperty(
                DatabaseConfig.PATH_PROPERTY,
                temporaryDirectory.resolve("dashboard-service-test.db").toString()
        );
        DatabaseInitializer.initialize();
        dashboardService = new DashboardService(new DashboardDao());
        familiaDao = new FamiliaDao();
        doacaoDao = new DoacaoDao();
        entregaDao = new EntregaDao();
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
    void loadsEmptyDashboardWithoutErrors() {
        DashboardResultado dashboard = dashboardService.load();

        assertEquals(0, dashboard.resumo().totalFamilias());
        assertEquals(0, dashboard.resumo().totalDoacoes());
        assertEquals(0, dashboard.resumo().totalEntregas());
        assertEquals(0, dashboard.resumo().familiasAtendidas());
        assertTrue(dashboard.atividadesRecentes().isEmpty());
    }

    @Test
    void reflectsOneRegisteredFamilyImmediately() {
        insertFamily("00000000001", "Família Única", LocalDate.of(2026, 9, 10));

        DashboardResultado dashboard = dashboardService.load();

        assertEquals(1, dashboard.resumo().totalFamilias());
        assertEquals(0, dashboard.resumo().totalDoacoes());
        assertEquals(0, dashboard.resumo().totalEntregas());
        assertEquals(0, dashboard.resumo().familiasAtendidas());
        assertEquals("familia", dashboard.atividadesRecentes().getFirst().tipo());
        assertEquals("Família Única", dashboard.atividadesRecentes().getFirst().titulo());
        assertNull(dashboard.atividadesRecentes().getFirst().quantidade());
    }

    @Test
    void calculatesControlledCountsAndDistinctAttendedFamilies() {
        Familia familyA = insertFamily("00000000001", "Família A", LocalDate.of(2026, 9, 1));
        Familia familyB = insertFamily("00000000002", "Família B", LocalDate.of(2026, 9, 2));
        insertFamily("00000000003", "Família C", LocalDate.of(2026, 9, 3));
        Doacao donationX = insertDonation("Alimentos", "Item X", 20, LocalDate.of(2026, 9, 4));
        Doacao donationY = insertDonation("Higiene", "Item Y", 20, LocalDate.of(2026, 9, 5));

        entregaDao.insertWithAvailabilityCheck(
                familyA.getId(), donationX.getId(), 2, LocalDate.of(2026, 9, 6), null
        );
        entregaDao.insertWithAvailabilityCheck(
                familyA.getId(), donationY.getId(), 3, LocalDate.of(2026, 9, 7), null
        );
        entregaDao.insertWithAvailabilityCheck(
                familyB.getId(), donationX.getId(), 4, LocalDate.of(2026, 9, 8), null
        );

        DashboardResultado dashboard = dashboardService.load();

        assertEquals(3, dashboard.resumo().totalFamilias());
        assertEquals(2, dashboard.resumo().totalDoacoes());
        assertEquals(3, dashboard.resumo().totalEntregas());
        assertEquals(2, dashboard.resumo().familiasAtendidas());
    }

    @Test
    void handlesFamiliesAndDonationsWithoutDeliveries() {
        insertFamily("00000000001", "Maria Silva", LocalDate.of(2026, 9, 10));
        insertDonation("Roupas", "Cobertores", 10, LocalDate.of(2026, 9, 11));

        DashboardResultado dashboard = dashboardService.load();

        assertEquals(1, dashboard.resumo().totalFamilias());
        assertEquals(1, dashboard.resumo().totalDoacoes());
        assertEquals(0, dashboard.resumo().totalEntregas());
        assertEquals(0, dashboard.resumo().familiasAtendidas());
        assertEquals(List.of("doacao", "familia"), dashboard.atividadesRecentes().stream()
                .map(DashboardAtividade::tipo)
                .toList());
    }

    @Test
    void loadsRecentDeliveryWithJoinedFamilyAndDonationData() {
        Familia family = insertFamily(
                "00000000001", "Maria São Luís", LocalDate.of(2026, 9, 1)
        );
        Doacao donation = insertDonation(
                "Alimentos", "Cesta básica", 10, LocalDate.of(2026, 9, 10)
        );
        entregaDao.insertWithAvailabilityCheck(
                family.getId(), donation.getId(), 3, LocalDate.of(2026, 9, 20), null
        );

        DashboardAtividade activity = dashboardService.load().atividadesRecentes().getFirst();

        assertEquals("entrega", activity.tipo());
        assertEquals("Maria São Luís", activity.titulo());
        assertEquals("Alimentos — Cesta básica", activity.detalhe());
        assertEquals(3, activity.quantidade());
        assertEquals(LocalDate.of(2026, 9, 20), activity.data());
    }

    @Test
    void limitsAndOrdersRecentActivitiesInTheDatabaseQuery() {
        for (int day = 1; day <= 6; day++) {
            insertDonation(
                    "Tipo " + day,
                    "Descrição " + day,
                    day,
                    LocalDate.of(2026, 9, day)
            );
        }

        List<DashboardAtividade> activities = dashboardService.load().atividadesRecentes();

        assertEquals(5, activities.size());
        assertEquals("Tipo 6", activities.getFirst().titulo());
        assertEquals(LocalDate.of(2026, 9, 6), activities.getFirst().data());
        assertEquals("Tipo 2", activities.getLast().titulo());
        assertEquals(LocalDate.of(2026, 9, 2), activities.getLast().data());
    }

    private Familia insertFamily(String cpf, String name, LocalDate date) {
        return familiaDao.insert(
                cpf, name, 4, "11999990000", "Rua A", "10", "Centro", "São Paulo",
                LocalDateTime.of(date, java.time.LocalTime.NOON)
        );
    }

    private Doacao insertDonation(
            String type,
            String description,
            int quantity,
            LocalDate date
    ) {
        return doacaoDao.insert(type, description, quantity, date, null);
    }
}
