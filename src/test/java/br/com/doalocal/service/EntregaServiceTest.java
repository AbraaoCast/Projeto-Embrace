package br.com.doalocal.service;

import br.com.doalocal.config.DatabaseConfig;
import br.com.doalocal.config.DatabaseInitializer;
import br.com.doalocal.dao.DoacaoDao;
import br.com.doalocal.dao.EntregaDao;
import br.com.doalocal.dao.FamiliaDao;
import br.com.doalocal.exception.DoacaoNotFoundException;
import br.com.doalocal.exception.FamiliaNotFoundException;
import br.com.doalocal.exception.InsufficientDonationQuantityException;
import br.com.doalocal.exception.ValidationException;
import br.com.doalocal.model.Doacao;
import br.com.doalocal.model.Entrega;
import br.com.doalocal.model.EntregaResumo;
import br.com.doalocal.model.Familia;
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
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntregaServiceTest {

    @TempDir
    Path temporaryDirectory;

    private String previousDatabasePath;
    private EntregaService entregaService;
    private FamiliaDao familiaDao;
    private DoacaoDao doacaoDao;

    @BeforeEach
    void setUp() throws Exception {
        previousDatabasePath = System.getProperty(DatabaseConfig.PATH_PROPERTY);
        System.setProperty(
                DatabaseConfig.PATH_PROPERTY,
                temporaryDirectory.resolve("entrega-service-test.db").toString()
        );
        DatabaseInitializer.initialize();
        familiaDao = new FamiliaDao();
        doacaoDao = new DoacaoDao();
        entregaService = new EntregaService(new EntregaDao(), familiaDao, doacaoDao);
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
    void registersValidDeliveryWithNormalizedObservation() {
        Familia family = insertFamily();
        Doacao donation = insertDonation(100);

        Entrega delivery = entregaService.register(
                String.valueOf(family.getId()), String.valueOf(donation.getId()),
                "20", "22/09/2026", "  Entregue pela manhã  "
        );

        assertTrue(delivery.getId() > 0);
        assertEquals(family.getId(), delivery.getFamiliaId());
        assertEquals(donation.getId(), delivery.getDoacaoId());
        assertEquals(20, delivery.getQuantidadeEntregue());
        assertEquals(LocalDate.of(2026, 9, 22), delivery.getDataEntrega());
        assertEquals("Entregue pela manhã", delivery.getObservacao());
    }

    @Test
    void acceptsOptionalObservation() {
        Familia family = insertFamily();
        Doacao donation = insertDonation(100);

        Entrega delivery = entregaService.register(
                String.valueOf(family.getId()), String.valueOf(donation.getId()),
                "20", "22/09/2026", "   "
        );
        assertNull(delivery.getObservacao());
    }

    @ParameterizedTest
    @MethodSource("invalidDeliveryData")
    void rejectsInvalidDeliveryData(
            String familyId,
            String donationId,
            String quantity,
            String date,
            String observation,
            String expectedCode
    ) {
        ValidationException exception = assertThrows(ValidationException.class, () ->
                entregaService.register(familyId, donationId, quantity, date, observation));
        assertEquals(expectedCode, exception.getErrorCode());
    }

    @Test
    void rejectsMissingFamilyAndDonation() {
        Familia family = insertFamily();
        Doacao donation = insertDonation(100);

        assertThrows(FamiliaNotFoundException.class, () -> entregaService.register(
                "999", String.valueOf(donation.getId()), "1", "22/09/2026", null
        ));
        assertThrows(DoacaoNotFoundException.class, () -> entregaService.register(
                String.valueOf(family.getId()), "999", "1", "22/09/2026", null
        ));
    }

    @Test
    void calculatesAndEnforcesAvailabilityAcrossDeliveries() {
        Familia family = insertFamily();
        Doacao donation = insertDonation(100);

        entregaService.register(
                String.valueOf(family.getId()), String.valueOf(donation.getId()),
                "20", "20/09/2026", null
        );
        assertEquals(80, available(donation.getId()));

        entregaService.register(
                String.valueOf(family.getId()), String.valueOf(donation.getId()),
                "30", "21/09/2026", null
        );
        assertEquals(50, available(donation.getId()));

        assertThrows(InsufficientDonationQuantityException.class, () -> entregaService.register(
                String.valueOf(family.getId()), String.valueOf(donation.getId()),
                "51", "22/09/2026", null
        ));
        assertEquals(50, available(donation.getId()));

        entregaService.register(
                String.valueOf(family.getId()), String.valueOf(donation.getId()),
                "50", "22/09/2026", null
        );
        assertEquals(0, available(donation.getId()));
        assertEquals(100, doacaoDao.findById(donation.getId()).orElseThrow().getQuantidade());
    }

    @Test
    void listsHistoryWithJoinedFamilyAndDonationData() {
        Familia family = insertFamily();
        Doacao donation = insertDonation(100);
        assertTrue(entregaService.listHistory().isEmpty());

        entregaService.register(
                String.valueOf(family.getId()), String.valueOf(donation.getId()),
                "10", "22/09/2026", "Teste"
        );

        List<EntregaResumo> history = entregaService.listHistory();
        assertEquals(1, history.size());
        assertEquals("Maria Silva", history.getFirst().nomeResponsavel());
        assertEquals("Cestas básicas", history.getFirst().descricaoDoacao());
        assertEquals(10, history.getFirst().quantidadeEntregue());
    }

    private Familia insertFamily() {
        return familiaDao.insert(
                "52998224725", "Maria Silva", 4, "11999990000", "Rua A", "10",
                "Centro", "São Paulo", LocalDateTime.now().withNano(0)
        );
    }

    private Doacao insertDonation(int quantity) {
        return doacaoDao.insert(
                "Alimentos", "Cestas básicas", quantity, LocalDate.of(2026, 9, 20), null
        );
    }

    private int available(long donationId) {
        return entregaService.listDonationAvailability().stream()
                .filter(item -> item.id() == donationId)
                .findFirst()
                .orElseThrow()
                .quantidadeDisponivel();
    }

    private static Stream<Arguments> invalidDeliveryData() {
        return Stream.of(
                Arguments.of(null, "1", "1", "22/09/2026", null, "family_required"),
                Arguments.of(" ", "1", "1", "22/09/2026", null, "family_required"),
                Arguments.of("1", null, "1", "22/09/2026", null, "donation_required"),
                Arguments.of("abc", "1", "1", "22/09/2026", null, "invalid_id"),
                Arguments.of("0", "1", "1", "22/09/2026", null, "invalid_id"),
                Arguments.of("1", "-1", "1", "22/09/2026", null, "invalid_id"),
                Arguments.of("1", "1", null, "22/09/2026", null, "quantity_required"),
                Arguments.of("1", "1", "abc", "22/09/2026", null, "invalid_quantity"),
                Arguments.of("1", "1", "0", "22/09/2026", null, "quantity_must_be_positive"),
                Arguments.of("1", "1", "-1", "22/09/2026", null, "quantity_must_be_positive"),
                Arguments.of("1", "1", "1000001", "22/09/2026", null, "quantity_too_large"),
                Arguments.of("1", "1", "1", null, null, "date_required"),
                Arguments.of("1", "1", "1", "2026-09-22", null, "invalid_date"),
                Arguments.of("1", "1", "1", "31/02/2026", null, "invalid_date"),
                Arguments.of("1", "1", "1", "99/99/9999", null, "invalid_date"),
                Arguments.of("1", "1", "1", "22/09/2026", "O".repeat(1001), "observation_too_long")
        );
    }
}
