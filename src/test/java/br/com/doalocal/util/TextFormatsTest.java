package br.com.doalocal.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextFormatsTest {

    @ParameterizedTest
    @CsvSource({
            "joão da silva,João Da Silva",
            "RUA DAS FLORES,Rua Das Flores",
            "são paulo,São Paulo",
            "são paulo,São Paulo",
            "avenida 9 de julho,Avenida 9 De Julho",
            "d'ávila,D'Ávila",
            "jardim-vista alegre,Jardim-Vista Alegre"
    })
    void convertsNamesAndAddressesToTitleCase(String input, String expected) {
        assertEquals(expected, TextFormats.titleCase(input));
    }
}
