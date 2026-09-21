package br.com.doalocal.dao;

import br.com.doalocal.config.ConnectionFactory;
import br.com.doalocal.config.DatabaseConfig;
import br.com.doalocal.config.DatabaseInitializer;
import br.com.doalocal.exception.DuplicateCpfException;
import br.com.doalocal.exception.RelatedRecordsException;
import br.com.doalocal.model.Familia;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FamiliaDaoTest {

    @TempDir
    Path temporaryDirectory;

    private String previousDatabasePath;
    private FamiliaDao familiaDao;

    @BeforeEach
    void setUp() throws Exception {
        previousDatabasePath = System.getProperty(DatabaseConfig.PATH_PROPERTY);
        System.setProperty(
                DatabaseConfig.PATH_PROPERTY,
                temporaryDirectory.resolve("familia-dao-test.db").toString()
        );
        DatabaseInitializer.initialize();
        familiaDao = new FamiliaDao();
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
    void insertsAndFindsByIdAndCpf() {
        Familia created = insert("52998224725", "Maria Silva");

        Familia byId = familiaDao.findById(created.getId()).orElseThrow();
        Familia byCpf = familiaDao.findByCpf("52998224725").orElseThrow();

        assertTrue(created.getId() > 0);
        assertEquals(created.getId(), byId.getId());
        assertEquals(created.getId(), byCpf.getId());
        assertEquals("Maria Silva", byId.getNomeResponsavel());
        assertEquals(4, byId.getQuantidadeMembros());
        assertEquals("Rua Central", byId.getLogradouro());
    }

    @Test
    void listsNoFamiliesThenMultipleFamiliesInNameOrder() {
        assertTrue(familiaDao.findAll().isEmpty());

        insert("52998224725", "Zélia Santos");
        insert("11144477735", "Ana Lima");

        List<Familia> familias = familiaDao.findAll();
        assertEquals(2, familias.size());
        assertEquals("Ana Lima", familias.get(0).getNomeResponsavel());
        assertEquals("Zélia Santos", familias.get(1).getNomeResponsavel());
    }

    @Test
    void updatesFieldsWithoutChangingRegistrationDate() {
        Familia created = insert("52998224725", "Maria Silva");

        assertTrue(familiaDao.update(
                created.getId(), "11144477735", "Maria Atualizada", 5, "(11) 98888-1111",
                "Avenida Nova", "20", "Centro", "Campinas"
        ));

        Familia updated = familiaDao.findById(created.getId()).orElseThrow();
        assertEquals("11144477735", updated.getCpfResponsavel());
        assertEquals("Maria Atualizada", updated.getNomeResponsavel());
        assertEquals(5, updated.getQuantidadeMembros());
        assertEquals(created.getDataCadastro(), updated.getDataCadastro());
    }

    @Test
    void deletesExistingFamilyAndReturnsFalseForMissingFamily() {
        Familia created = insert("52998224725", "Maria Silva");

        assertTrue(familiaDao.delete(created.getId()));
        assertFalse(familiaDao.findById(created.getId()).isPresent());
        assertFalse(familiaDao.delete(created.getId()));
    }

    @Test
    void uniqueConstraintRejectsDuplicateCpf() {
        insert("52998224725", "Maria Silva");

        assertThrows(DuplicateCpfException.class,
                () -> insert("52998224725", "Outra Pessoa"));
    }

    @Test
    void foreignKeyPreventsDeletingFamilyWithDelivery() throws Exception {
        Familia family = insert("52998224725", "Maria Silva");

        try (Connection connection = ConnectionFactory.openConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO doacao (tipo, quantidade, data_doacao)
                    VALUES ('Alimento', 10, '2026-09-20')
                    """);
        }

        try (Connection connection = ConnectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO entrega (id_familia, id_doacao, quantidade_entregue, data_entrega)
                     VALUES (?, 1, 1, '2026-09-20')
                     """)) {
            statement.setLong(1, family.getId());
            statement.executeUpdate();
        }

        assertThrows(RelatedRecordsException.class, () -> familiaDao.delete(family.getId()));
        assertTrue(familiaDao.findById(family.getId()).isPresent());
    }

    private Familia insert(String cpf, String nome) {
        return familiaDao.insert(
                cpf, nome, 4, "(11) 99999-0000", "Rua Central", "10", "Centro", "São Paulo",
                LocalDateTime.now().withNano(0)
        );
    }
}
