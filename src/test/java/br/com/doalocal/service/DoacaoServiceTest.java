package br.com.doalocal.service;

import br.com.doalocal.config.DatabaseConfig;
import br.com.doalocal.config.DatabaseInitializer;
import br.com.doalocal.dao.DoacaoDao;
import br.com.doalocal.exception.DoacaoNotFoundException;
import br.com.doalocal.exception.ValidationException;
import br.com.doalocal.model.Doacao;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DoacaoServiceTest {

    @TempDir
    Path temporaryDirectory;

    private String previousDatabasePath;
    private DoacaoService doacaoService;

    @BeforeEach
    void setUp() throws Exception {
        previousDatabasePath = System.getProperty(DatabaseConfig.PATH_PROPERTY);
        System.setProperty(
                DatabaseConfig.PATH_PROPERTY,
                temporaryDirectory.resolve("doacao-service-test.db").toString()
        );
        DatabaseInitializer.initialize();
        doacaoService = new DoacaoService(new DoacaoDao());
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
    void createsValidDonationWithNormalizedFields() {
        Doacao donation = doacaoService.create(
                "  Alimentos  ", "  Cestas básicas  ", "10", "2026-09-21", "  Sem glúten  "
        );

        assertTrue(donation.getId() > 0);
        assertEquals("Alimentos", donation.getTipo());
        assertEquals("Cestas básicas", donation.getDescricao());
        assertEquals(10, donation.getQuantidade());
        assertEquals(LocalDate.of(2026, 9, 21), donation.getDataDoacao());
        assertEquals("Sem glúten", donation.getObservacao());
    }

    @Test
    void acceptsMissingOrBlankOptionalObservationAsNull() {
        assertNull(createWithObservation(null).getObservacao());
        assertNull(createWithObservation("   ").getObservacao());
    }

    @ParameterizedTest
    @MethodSource("invalidDonationData")
    void rejectsInvalidDonationData(
            String tipo,
            String descricao,
            String quantidade,
            String data,
            String observacao,
            String expectedCode
    ) {
        ValidationException exception = assertThrows(ValidationException.class, () ->
                doacaoService.create(tipo, descricao, quantidade, data, observacao));
        assertEquals(expectedCode, exception.getErrorCode());
    }

    @Test
    void listsAndFindsDonations() {
        Doacao first = doacaoService.create(
                "Roupas", "Camisetas", "20", "2026-09-20", null
        );
        doacaoService.create("Alimentos", "Arroz", "30", "2026-09-21", null);

        List<Doacao> doacoes = doacaoService.listAll();
        assertEquals(2, doacoes.size());
        assertEquals("Arroz", doacoes.get(0).getDescricao());
        assertEquals(first.getId(), doacaoService.findById(String.valueOf(first.getId())).getId());
    }

    @Test
    void rejectsInvalidAndMissingIds() {
        for (String id : new String[]{null, "", "abc", "0", "-1"}) {
            ValidationException exception = assertThrows(
                    ValidationException.class, () -> doacaoService.findById(id)
            );
            assertEquals("invalid_id", exception.getErrorCode());
        }
        assertThrows(DoacaoNotFoundException.class, () -> doacaoService.findById("999"));
    }

    @Test
    void updatesDonationAfterValidatingAllFields() {
        Doacao created = doacaoService.create(
                "Outros", "Cobertor", "5", "2026-09-20", null
        );

        Doacao updated = doacaoService.update(
                String.valueOf(created.getId()), "Roupas", "Cobertores", "8",
                "2026-09-21", "Tamanho casal"
        );

        assertEquals("Roupas", updated.getTipo());
        assertEquals("Cobertores", updated.getDescricao());
        assertEquals(8, updated.getQuantidade());
        assertEquals(LocalDate.of(2026, 9, 21), updated.getDataDoacao());
        assertEquals("Tamanho casal", updated.getObservacao());
    }

    @Test
    void rejectsInvalidUpdateAndMissingDonation() {
        Doacao created = doacaoService.create(
                "Alimentos", "Arroz", "10", "2026-09-21", null
        );

        ValidationException invalid = assertThrows(ValidationException.class, () ->
                doacaoService.update(
                        String.valueOf(created.getId()), "Alimentos", "Arroz", "0",
                        "2026-09-21", null
                ));
        assertEquals("quantity_must_be_positive", invalid.getErrorCode());

        assertThrows(DoacaoNotFoundException.class, () ->
                doacaoService.update(
                        "999", "Alimentos", "Arroz", "10", "2026-09-21", null
                ));
    }

    @Test
    void deletesExistingDonationAndRejectsMissingOrInvalidId() {
        Doacao created = doacaoService.create(
                "Alimentos", "Feijão", "10", "2026-09-21", null
        );
        doacaoService.delete(String.valueOf(created.getId()));

        assertThrows(DoacaoNotFoundException.class,
                () -> doacaoService.findById(String.valueOf(created.getId())));
        assertThrows(DoacaoNotFoundException.class, () -> doacaoService.delete("999"));
        assertThrows(ValidationException.class, () -> doacaoService.delete("abc"));
    }

    private Doacao createWithObservation(String observation) {
        return doacaoService.create(
                "Alimentos", "Cesta básica", "10", "2026-09-21", observation
        );
    }

    private static Stream<Arguments> invalidDonationData() {
        return Stream.of(
                Arguments.of(null, "Arroz", "10", "2026-09-21", null, "type_required"),
                Arguments.of("   ", "Arroz", "10", "2026-09-21", null, "type_required"),
                Arguments.of("T".repeat(51), "Arroz", "10", "2026-09-21", null, "type_too_long"),
                Arguments.of("Alimentos", " ", "10", "2026-09-21", null, "description_required"),
                Arguments.of("Alimentos", "D".repeat(201), "10", "2026-09-21", null, "description_too_long"),
                Arguments.of("Alimentos", "Arroz", null, "2026-09-21", null, "quantity_required"),
                Arguments.of("Alimentos", "Arroz", "abc", "2026-09-21", null, "invalid_quantity"),
                Arguments.of("Alimentos", "Arroz", "0", "2026-09-21", null, "quantity_must_be_positive"),
                Arguments.of("Alimentos", "Arroz", "-5", "2026-09-21", null, "quantity_must_be_positive"),
                Arguments.of("Alimentos", "Arroz", "1000001", "2026-09-21", null, "quantity_too_large"),
                Arguments.of("Alimentos", "Arroz", "10", "", null, "date_required"),
                Arguments.of("Alimentos", "Arroz", "10", "21/09/2026", null, "invalid_date"),
                Arguments.of("Alimentos", "Arroz", "10", "2026-02-30", null, "invalid_date"),
                Arguments.of("Alimentos", "Arroz", "10", "2026-09-21", "O".repeat(1001), "observation_too_long")
        );
    }
}
