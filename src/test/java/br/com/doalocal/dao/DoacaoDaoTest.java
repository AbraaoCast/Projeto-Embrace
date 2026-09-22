package br.com.doalocal.dao;

import br.com.doalocal.config.ConnectionFactory;
import br.com.doalocal.config.DatabaseConfig;
import br.com.doalocal.config.DatabaseInitializer;
import br.com.doalocal.exception.RelatedDonationRecordsException;
import br.com.doalocal.model.Doacao;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DoacaoDaoTest {

    @TempDir
    Path temporaryDirectory;

    private String previousDatabasePath;
    private DoacaoDao doacaoDao;

    @BeforeEach
    void setUp() throws Exception {
        previousDatabasePath = System.getProperty(DatabaseConfig.PATH_PROPERTY);
        System.setProperty(
                DatabaseConfig.PATH_PROPERTY,
                temporaryDirectory.resolve("doacao-dao-test.db").toString()
        );
        DatabaseInitializer.initialize();
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
    void insertsAndFindsDonationById() {
        Doacao created = insert("Alimentos", "Cestas básicas", 10, LocalDate.of(2026, 9, 21), null);
        Doacao found = doacaoDao.findById(created.getId()).orElseThrow();

        assertTrue(created.getId() > 0);
        assertEquals("Alimentos", found.getTipo());
        assertEquals("Cestas básicas", found.getDescricao());
        assertEquals(10, found.getQuantidade());
        assertEquals(LocalDate.of(2026, 9, 21), found.getDataDoacao());
        assertNull(found.getObservacao());
    }

    @Test
    void listsEmptyAndMultipleDonationsNewestFirst() {
        assertTrue(doacaoDao.findAll().isEmpty());

        insert("Roupas", "Camisetas", 20, LocalDate.of(2026, 9, 20), "Adulto");
        insert("Alimentos", "Arroz", 30, LocalDate.of(2026, 9, 21), null);

        List<Doacao> doacoes = doacaoDao.findAll();
        assertEquals(2, doacoes.size());
        assertEquals("Arroz", doacoes.get(0).getDescricao());
        assertEquals("Camisetas", doacoes.get(1).getDescricao());
    }

    @Test
    void updatesDonation() {
        Doacao created = insert("Outros", "Cobertor", 5, LocalDate.of(2026, 9, 20), null);

        assertTrue(doacaoDao.update(
                created.getId(), "Roupas", "Cobertores", 8,
                LocalDate.of(2026, 9, 21), "Tamanho casal"
        ));

        Doacao updated = doacaoDao.findById(created.getId()).orElseThrow();
        assertEquals("Roupas", updated.getTipo());
        assertEquals("Cobertores", updated.getDescricao());
        assertEquals(8, updated.getQuantidade());
        assertEquals("Tamanho casal", updated.getObservacao());
    }

    @Test
    void deletesExistingDonationAndReturnsFalseForMissingDonation() {
        Doacao created = insert("Alimentos", "Feijão", 10, LocalDate.of(2026, 9, 21), null);

        assertTrue(doacaoDao.delete(created.getId()));
        assertFalse(doacaoDao.findById(created.getId()).isPresent());
        assertFalse(doacaoDao.delete(created.getId()));
    }

    @Test
    void foreignKeyPreventsDeletingDonationWithDelivery() throws Exception {
        Doacao donation = insert("Alimentos", "Arroz", 10, LocalDate.of(2026, 9, 21), null);
        FamiliaDao familiaDao = new FamiliaDao();
        var family = familiaDao.insert(
                "52998224725", "Maria Silva", 4, "11999990000", "Rua A", "10",
                "Centro", "São Paulo", LocalDateTime.now().withNano(0)
        );

        try (Connection connection = ConnectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO entrega (id_familia, id_doacao, quantidade_entregue, data_entrega)
                     VALUES (?, ?, 1, '2026-09-21')
                     """)) {
            statement.setLong(1, family.getId());
            statement.setLong(2, donation.getId());
            statement.executeUpdate();
        }

        assertThrows(RelatedDonationRecordsException.class, () -> doacaoDao.delete(donation.getId()));
        assertTrue(doacaoDao.findById(donation.getId()).isPresent());
    }

    private Doacao insert(
            String tipo,
            String descricao,
            int quantidade,
            LocalDate data,
            String observacao
    ) {
        return doacaoDao.insert(tipo, descricao, quantidade, data, observacao);
    }
}
