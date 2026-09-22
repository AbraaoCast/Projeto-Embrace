package br.com.doalocal.service;

import br.com.doalocal.config.DatabaseConfig;
import br.com.doalocal.config.DatabaseInitializer;
import br.com.doalocal.dao.FamiliaDao;
import br.com.doalocal.exception.DuplicateCpfException;
import br.com.doalocal.exception.FamiliaNotFoundException;
import br.com.doalocal.exception.ValidationException;
import br.com.doalocal.model.Familia;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FamiliaServiceTest {

    @TempDir
    Path temporaryDirectory;

    private String previousDatabasePath;
    private FamiliaService familiaService;

    @BeforeEach
    void setUp() throws Exception {
        previousDatabasePath = System.getProperty(DatabaseConfig.PATH_PROPERTY);
        System.setProperty(
                DatabaseConfig.PATH_PROPERTY,
                temporaryDirectory.resolve("familia-service-test.db").toString()
        );
        DatabaseInitializer.initialize();
        familiaService = new FamiliaService(new FamiliaDao());
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
    void createsFamilyWithNormalizedDataAndBackendDate() {
        Familia family = create("  Maria Silva  ", "529.982.247-25", "4");

        assertTrue(family.getId() > 0);
        assertEquals("Maria Silva", family.getNomeResponsavel());
        assertEquals("52998224725", family.getCpfResponsavel());
        assertEquals("11999990000", family.getTelefone());
        assertEquals(4, family.getQuantidadeMembros());
        assertTrue(family.getDataCadastro() != null);
    }

    @ParameterizedTest
    @MethodSource("invalidFamilyData")
    void rejectsInvalidFamilyData(
            String nome,
            String cpf,
            String members,
            String telefone,
            String logradouro,
            String bairro,
            String cidade,
            String expectedCode
    ) {
        ValidationException exception = assertThrows(ValidationException.class, () ->
                familiaService.create(
                        nome, cpf, members, telefone, logradouro, "10", bairro, cidade
                ));
        assertEquals(expectedCode, exception.getErrorCode());
    }

    @Test
    void rejectsDuplicateCpfAfterNormalization() {
        create("Maria Silva", "529.982.247-25", "4");

        assertThrows(DuplicateCpfException.class,
                () -> create("Outra Pessoa", "52998224725", "2"));
    }

    @Test
    void listsAndFindsFamilies() {
        Familia maria = create("Maria Silva", "52998224725", "4");
        create("Ana Lima", "11144477735", "2");

        List<Familia> families = familiaService.listAll();
        assertEquals(2, families.size());
        assertEquals(maria.getId(), familiaService.findById(String.valueOf(maria.getId())).getId());
    }

    @Test
    void rejectsInvalidAndMissingIds() {
        for (String id : new String[]{null, "", "abc", "0", "-1"}) {
            ValidationException exception = assertThrows(
                    ValidationException.class, () -> familiaService.findById(id)
            );
            assertEquals("invalid_id", exception.getErrorCode());
        }
        assertThrows(FamiliaNotFoundException.class, () -> familiaService.findById("999"));
    }

    @Test
    void updatesKeepingSameCpfAndOriginalDate() {
        Familia created = create("Maria Silva", "52998224725", "4");

        Familia updated = familiaService.update(
                String.valueOf(created.getId()), "Maria Souza", "529.982.247-25", "5",
                "(11) 98888-7777", "Rua Nova", "22", "Centro", "São Paulo"
        );

        assertEquals("Maria Souza", updated.getNomeResponsavel());
        assertEquals("52998224725", updated.getCpfResponsavel());
        assertEquals("11988887777", updated.getTelefone());
        assertEquals(created.getDataCadastro(), updated.getDataCadastro());
    }

    @Test
    void updatesCpfWhenItDoesNotBelongToAnotherFamily() {
        Familia created = create("Maria Silva", "52998224725", "4");

        Familia updated = familiaService.update(
                String.valueOf(created.getId()), "Maria Silva", "11144477735", "4",
                "(11) 99999-0000", "Rua Central", "10", "Centro", "São Paulo"
        );

        assertEquals("11144477735", updated.getCpfResponsavel());
    }

    @Test
    void rejectsCpfOwnedByAnotherFamilyDuringUpdate() {
        Familia first = create("Maria Silva", "52998224725", "4");
        create("Ana Lima", "11144477735", "2");

        assertThrows(DuplicateCpfException.class, () -> familiaService.update(
                String.valueOf(first.getId()), "Maria Silva", "11144477735", "4",
                "(11) 99999-0000", "Rua Central", "10", "Centro", "São Paulo"
        ));
    }

    @Test
    void rejectsInvalidUpdateAndMissingFamily() {
        Familia family = create("Maria Silva", "52998224725", "4");
        assertThrows(ValidationException.class, () -> familiaService.update(
                String.valueOf(family.getId()), " ", "52998224725", "4",
                "(11) 99999-0000", "Rua Central", "10", "Centro", "São Paulo"
        ));
        assertThrows(FamiliaNotFoundException.class, () -> familiaService.update(
                "999", "Maria", "52998224725", "4",
                "(11) 99999-0000", "Rua Central", "10", "Centro", "São Paulo"
        ));
    }

    @Test
    void deletesExistingFamilyAndRejectsMissingOrInvalidId() {
        Familia family = create("Maria Silva", "52998224725", "4");
        familiaService.delete(String.valueOf(family.getId()));

        assertThrows(FamiliaNotFoundException.class,
                () -> familiaService.findById(String.valueOf(family.getId())));
        assertThrows(FamiliaNotFoundException.class, () -> familiaService.delete("999"));
        assertThrows(ValidationException.class, () -> familiaService.delete("abc"));
    }

    private Familia create(String nome, String cpf, String members) {
        return familiaService.create(
                nome, cpf, members, "(11) 99999-0000", "Rua Central", "10", "Centro", "São Paulo"
        );
    }

    private static Stream<Arguments> invalidFamilyData() {
        return Stream.of(
                Arguments.of(" ", "52998224725", "4", "11999990000", "Rua", "Centro", "São Paulo", "name_required"),
                Arguments.of("Maria", null, "4", "11999990000", "Rua", "Centro", "São Paulo", "cpf_required"),
                Arguments.of("Maria", "123.456.789-00", "4", "11999990000", "Rua", "Centro", "São Paulo", "invalid_cpf"),
                Arguments.of("Maria", "11111111111", "4", "11999990000", "Rua", "Centro", "São Paulo", "invalid_cpf"),
                Arguments.of("Maria", "52998224725", "", "11999990000", "Rua", "Centro", "São Paulo", "members_required"),
                Arguments.of("Maria", "52998224725", "texto", "11999990000", "Rua", "Centro", "São Paulo", "invalid_members"),
                Arguments.of("Maria", "52998224725", "0", "11999990000", "Rua", "Centro", "São Paulo", "members_must_be_positive"),
                Arguments.of("Maria", "52998224725", "-2", "11999990000", "Rua", "Centro", "São Paulo", "members_must_be_positive"),
                Arguments.of("Maria", "52998224725", "101", "11999990000", "Rua", "Centro", "São Paulo", "members_too_large"),
                Arguments.of("Maria", "52998224725", "4", " ", "Rua", "Centro", "São Paulo", "phone_required"),
                Arguments.of("Maria", "52998224725", "4", "abc", "Rua", "Centro", "São Paulo", "invalid_phone"),
                Arguments.of("Maria", "52998224725", "4", "123", "Rua", "Centro", "São Paulo", "invalid_phone"),
                Arguments.of("Maria", "52998224725", "4", "(98) 999", "Rua", "Centro", "São Paulo", "invalid_phone"),
                Arguments.of("Maria", "52998224725", "4", "(98) abcde-9999", "Rua", "Centro", "São Paulo", "invalid_phone"),
                Arguments.of("Maria", "52998224725", "4", "11999990000", " ", "Centro", "São Paulo", "address_required"),
                Arguments.of("Maria", "52998224725", "4", "11999990000", "Rua", " ", "São Paulo", "district_required"),
                Arguments.of("Maria", "52998224725", "4", "11999990000", "Rua", "Centro", " ", "city_required")
        );
    }
}
