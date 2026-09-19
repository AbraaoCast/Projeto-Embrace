package br.com.doalocal.dao;

import br.com.doalocal.config.DatabaseConfig;
import br.com.doalocal.config.DatabaseInitializer;
import br.com.doalocal.exception.DuplicateEmailException;
import br.com.doalocal.model.Administrador;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdministradorDaoTest {

    @TempDir
    Path temporaryDirectory;

    private String previousDatabasePath;
    private AdministradorDao administradorDao;

    @BeforeEach
    void setUp() throws Exception {
        previousDatabasePath = System.getProperty(DatabaseConfig.PATH_PROPERTY);
        System.setProperty(
                DatabaseConfig.PATH_PROPERTY,
                temporaryDirectory.resolve("administrador-dao-test.db").toString()
        );
        DatabaseInitializer.initialize();
        administradorDao = new AdministradorDao();
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
    void insertsAndFindsAdministratorByEmail() {
        Administrador created = administradorDao.insert(
                "Ana Silva",
                "ana@example.com",
                "pbkdf2_sha256$600000$salt$hash"
        );

        Administrador found = administradorDao.findByEmail("ana@example.com").orElseThrow();

        assertTrue(created.getId() > 0);
        assertEquals(created.getId(), found.getId());
        assertEquals("Ana Silva", found.getNome());
        assertEquals("ana@example.com", found.getEmail());
        assertEquals("pbkdf2_sha256$600000$salt$hash", found.getSenhaHash());
    }

    @Test
    void databaseUniqueConstraintRejectsDuplicateEmailIgnoringCase() {
        administradorDao.insert("Ana", "ana@example.com", "hash-1");

        assertThrows(
                DuplicateEmailException.class,
                () -> administradorDao.insert("Outra Ana", "ANA@EXAMPLE.COM", "hash-2")
        );
    }
}
