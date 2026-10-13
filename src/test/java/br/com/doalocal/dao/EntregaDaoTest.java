package br.com.doalocal.dao;

import br.com.doalocal.config.DatabaseConfig;
import br.com.doalocal.config.DatabaseInitializer;
import br.com.doalocal.exception.InsufficientDonationQuantityException;
import br.com.doalocal.exception.DonationQuantityBelowDeliveredException;
import br.com.doalocal.model.Doacao;
import br.com.doalocal.model.DoacaoDisponivel;
import br.com.doalocal.model.DoacaoEstoque;
import br.com.doalocal.model.Entrega;
import br.com.doalocal.model.EntregaResumo;
import br.com.doalocal.model.Familia;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntregaDaoTest {

    @TempDir
    Path temporaryDirectory;

    private String previousDatabasePath;
    private EntregaDao entregaDao;
    private FamiliaDao familiaDao;
    private DoacaoDao doacaoDao;

    @BeforeEach
    void setUp() throws Exception {
        previousDatabasePath = System.getProperty(DatabaseConfig.PATH_PROPERTY);
        System.setProperty(
                DatabaseConfig.PATH_PROPERTY,
                temporaryDirectory.resolve("entrega-dao-test.db").toString()
        );
        DatabaseInitializer.initialize();
        entregaDao = new EntregaDao();
        familiaDao = new FamiliaDao();
        doacaoDao = new DoacaoDao();
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
    void insertsDeliveryAndReturnsHumanReadableSummary() {
        Familia family = insertFamily("52998224725", "Maria Silva");
        Doacao donation = insertDonation("Alimentos", "Cestas básicas", 100);

        Entrega delivery = entregaDao.insertWithAvailabilityCheck(
                family.getId(), donation.getId(), 20, LocalDate.of(2026, 9, 22), null
        );
        EntregaResumo summary = entregaDao.findAllSummaries().getFirst();

        assertTrue(delivery.getId() > 0);
        assertEquals(family.getId(), delivery.getFamiliaId());
        assertEquals(donation.getId(), delivery.getDoacaoId());
        assertEquals("Maria Silva", summary.nomeResponsavel());
        assertEquals("Cestas básicas", summary.descricaoDoacao());
        assertEquals(20, summary.quantidadeEntregue());
        assertNull(summary.observacao());
    }

    @Test
    void calculatesAvailabilityWithoutChangingReceivedQuantity() {
        Familia family = insertFamily("52998224725", "Maria Silva");
        Doacao donation = insertDonation("Alimentos", "Arroz", 100);

        assertAvailability(donation.getId(), 100, 0, 100);
        entregaDao.insertWithAvailabilityCheck(
                family.getId(), donation.getId(), 20, LocalDate.of(2026, 9, 20), null
        );
        assertAvailability(donation.getId(), 100, 20, 80);
        entregaDao.insertWithAvailabilityCheck(
                family.getId(), donation.getId(), 30, LocalDate.of(2026, 9, 21), null
        );
        assertAvailability(donation.getId(), 100, 50, 50);
        assertEquals(100, doacaoDao.findById(donation.getId()).orElseThrow().getQuantidade());
    }

    @Test
    void allowsExactAvailableQuantityAndRejectsNegativeBalance() {
        Familia family = insertFamily("52998224725", "Maria Silva");
        Doacao donation = insertDonation("Alimentos", "Feijão", 5);

        entregaDao.insertWithAvailabilityCheck(
                family.getId(), donation.getId(), 5, LocalDate.of(2026, 9, 22), null
        );
        assertAvailability(donation.getId(), 5, 5, 0);

        InsufficientDonationQuantityException exception = assertThrows(
                InsufficientDonationQuantityException.class,
                () -> entregaDao.insertWithAvailabilityCheck(
                        family.getId(), donation.getId(), 1, LocalDate.of(2026, 9, 22), null
                )
        );
        assertEquals(0, exception.getAvailableQuantity());
        assertEquals(1, entregaDao.findAllSummaries().size());
    }

    @Test
    void rejectsQuantityAboveAvailableAndRollsBack() {
        Familia family = insertFamily("52998224725", "Maria Silva");
        Doacao donation = insertDonation("Alimentos", "Leite", 5);

        InsufficientDonationQuantityException exception = assertThrows(
                InsufficientDonationQuantityException.class,
                () -> entregaDao.insertWithAvailabilityCheck(
                        family.getId(), donation.getId(), 6, LocalDate.of(2026, 9, 22), null
                )
        );

        assertEquals(5, exception.getAvailableQuantity());
        assertTrue(entregaDao.findAllSummaries().isEmpty());
        assertAvailability(donation.getId(), 5, 0, 5);
    }

    @Test
    void preventsReducingOriginalQuantityBelowDeliveredTotal() {
        Familia family = insertFamily("52998224725", "Maria Silva");
        Doacao donation = insertDonation("Roupas", "Camisetas", 5);
        entregaDao.insertWithAvailabilityCheck(
                family.getId(), donation.getId(), 3, LocalDate.of(2026, 9, 22), null
        );

        DonationQuantityBelowDeliveredException exception = assertThrows(
                DonationQuantityBelowDeliveredException.class,
                () -> doacaoDao.update(
                        donation.getId(), "Roupas", "Camisetas", 2,
                        LocalDate.of(2026, 9, 20), null
                )
        );

        assertEquals(3, exception.getDeliveredQuantity());
        assertEquals(5, doacaoDao.findById(donation.getId()).orElseThrow().getQuantidade());
        assertAvailability(donation.getId(), 5, 3, 2);
    }

    @Test
    void serializesSimultaneousDeliveriesWithoutCreatingNegativeStock() throws Exception {
        Familia family = insertFamily("52998224725", "Maria Silva");
        Doacao donation = insertDonation("Alimentos", "Arroz", 5);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Boolean> first = executor.submit(() -> attemptConcurrentDelivery(
                    ready, start, family.getId(), donation.getId()
            ));
            Future<Boolean> second = executor.submit(() -> attemptConcurrentDelivery(
                    ready, start, family.getId(), donation.getId()
            ));

            ready.await();
            start.countDown();

            int successes = (first.get() ? 1 : 0) + (second.get() ? 1 : 0);
            assertEquals(1, successes);
        }

        assertAvailability(donation.getId(), 5, 4, 1);
        assertEquals(1, entregaDao.findAllSummaries().size());
    }

    @Test
    void listsEmptyAndMultipleDeliveriesNewestFirst() {
        assertTrue(entregaDao.findAllSummaries().isEmpty());
        Familia maria = insertFamily("52998224725", "Maria Silva");
        Familia ana = insertFamily("11144477735", "Ana Lima");
        Doacao donation = insertDonation("Roupas", "Cobertores", 20);

        entregaDao.insertWithAvailabilityCheck(
                maria.getId(), donation.getId(), 2, LocalDate.of(2026, 9, 20), "Primeira"
        );
        entregaDao.insertWithAvailabilityCheck(
                ana.getId(), donation.getId(), 3, LocalDate.of(2026, 9, 22), "Segunda"
        );

        List<EntregaResumo> history = entregaDao.findAllSummaries();
        assertEquals(2, history.size());
        assertEquals("Ana Lima", history.get(0).nomeResponsavel());
        assertEquals("Segunda", history.get(0).observacao());
        assertEquals("Maria Silva", history.get(1).nomeResponsavel());
    }

    private Familia insertFamily(String cpf, String name) {
        return familiaDao.insert(
                cpf, name, 4, "11999990000", "Rua A", "10", "Centro", "São Paulo",
                LocalDateTime.now().withNano(0)
        );
    }

    private Doacao insertDonation(String type, String description, int quantity) {
        return doacaoDao.insert(
                type, description, quantity, LocalDate.of(2026, 9, 20), null
        );
    }

    private void assertAvailability(long donationId, int received, int delivered, int available) {
        DoacaoDisponivel balance = entregaDao.findDonationAvailability().stream()
                .filter(item -> item.id() == donationId)
                .findFirst()
                .orElseThrow();
        assertEquals(received, balance.quantidadeRecebida());
        assertEquals(delivered, balance.quantidadeEntregue());
        assertEquals(available, balance.quantidadeDisponivel());

        DoacaoEstoque stock = doacaoDao.findAllStock().stream()
                .filter(item -> item.id() == donationId)
                .findFirst()
                .orElseThrow();
        assertEquals(received, stock.quantidadeOriginal());
        assertEquals(delivered, stock.quantidadeEntregue());
        assertEquals(available, stock.quantidadeDisponivel());
    }

    private boolean attemptConcurrentDelivery(
            CountDownLatch ready,
            CountDownLatch start,
            long familyId,
            long donationId
    ) throws InterruptedException {
        ready.countDown();
        start.await();
        try {
            new EntregaDao().insertWithAvailabilityCheck(
                    familyId, donationId, 4, LocalDate.of(2026, 9, 22), null
            );
            return true;
        } catch (InsufficientDonationQuantityException exception) {
            return false;
        }
    }
}
