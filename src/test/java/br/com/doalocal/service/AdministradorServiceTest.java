package br.com.doalocal.service;

import br.com.doalocal.config.DatabaseConfig;
import br.com.doalocal.config.DatabaseInitializer;
import br.com.doalocal.dao.AdministradorDao;
import br.com.doalocal.exception.DuplicateEmailException;
import br.com.doalocal.exception.InvalidCredentialsException;
import br.com.doalocal.exception.ValidationException;
import br.com.doalocal.model.Administrador;
import br.com.doalocal.model.AdministradorAutenticado;
import br.com.doalocal.util.PasswordHasher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdministradorServiceTest {

    @TempDir
    Path temporaryDirectory;

    private String previousDatabasePath;
    private AdministradorDao administradorDao;
    private AdministradorService administradorService;
    private PasswordHasher passwordHasher;

    @BeforeEach
    void setUp() throws Exception {
        previousDatabasePath = System.getProperty(DatabaseConfig.PATH_PROPERTY);
        System.setProperty(
                DatabaseConfig.PATH_PROPERTY,
                temporaryDirectory.resolve("administrador-service-test.db").toString()
        );
        DatabaseInitializer.initialize();
        administradorDao = new AdministradorDao();
        passwordHasher = new PasswordHasher();
        administradorService = new AdministradorService(administradorDao, passwordHasher);
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
    void registersValidAdministratorWithNormalizedEmailAndPasswordHash() {
        AdministradorAutenticado created = administradorService.register(
                "  Ana Silva  ",
                "  ANA@Example.COM ",
                "Senha123",
                "Senha123"
        );

        Administrador persisted = administradorDao.findByEmail("ana@example.com").orElseThrow();

        assertTrue(created.id() > 0);
        assertEquals("Ana Silva", persisted.getNome());
        assertEquals("ana@example.com", persisted.getEmail());
        assertNotEquals("Senha123", persisted.getSenhaHash());
        assertTrue(persisted.getSenhaHash().startsWith("pbkdf2_sha256$600000$"));
        assertTrue(passwordHasher.verify("Senha123", persisted.getSenhaHash()));
        assertFalse(passwordHasher.verify("SenhaErrada123", persisted.getSenhaHash()));
    }

    @ParameterizedTest
    @MethodSource("invalidRegistrationData")
    void rejectsInvalidRegistrationData(
            String name,
            String email,
            String password,
            String confirmation,
            String expectedCode
    ) {
        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> administradorService.register(name, email, password, confirmation)
        );

        assertEquals(expectedCode, exception.getErrorCode());
    }

    @Test
    void rejectsDuplicateEmailAfterNormalization() {
        administradorService.register("Ana", "ana@example.com", "Senha123", "Senha123");

        assertThrows(
                DuplicateEmailException.class,
                () -> administradorService.register(
                        "Outra Ana",
                        " ANA@EXAMPLE.COM ",
                        "OutraSenha123",
                        "OutraSenha123"
                )
        );
    }

    @Test
    void authenticatesOnlyWithCorrectCredentials() {
        administradorService.register("Ana", "ana@example.com", "Senha123", "Senha123");

        AdministradorAutenticado authenticated = administradorService.authenticate(
                " ANA@EXAMPLE.COM ",
                "Senha123"
        );
        assertEquals("Ana", authenticated.nome());
        assertEquals("ana@example.com", authenticated.email());

        assertThrows(
                InvalidCredentialsException.class,
                () -> administradorService.authenticate("inexistente@example.com", "Senha123")
        );
        assertThrows(
                InvalidCredentialsException.class,
                () -> administradorService.authenticate("ana@example.com", "SenhaIncorreta123")
        );
        assertThrows(
                InvalidCredentialsException.class,
                () -> administradorService.authenticate("ana@example.com", "S".repeat(129))
        );

        ValidationException missingFields = assertThrows(
                ValidationException.class,
                () -> administradorService.authenticate(" ", "")
        );
        assertEquals("login_required", missingFields.getErrorCode());
    }

    private static Stream<Arguments> invalidRegistrationData() {
        return Stream.of(
                Arguments.of(" ", "ana@example.com", "Senha123", "Senha123", "required_fields"),
                Arguments.of("Ana", " ", "Senha123", "Senha123", "required_fields"),
                Arguments.of("Ana", "email-invalido", "Senha123", "Senha123", "invalid_email"),
                Arguments.of("Ana", "ana@example.com", "curta1", "curta1", "weak_password"),
                Arguments.of("Ana", "ana@example.com", "SemNumero", "SemNumero", "weak_password"),
                Arguments.of("Ana", "ana@example.com", "Senha123", "Senha456", "password_mismatch")
        );
    }
}
