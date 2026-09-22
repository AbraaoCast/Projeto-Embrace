package br.com.doalocal.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DateFormatsTest {

    @ParameterizedTest
    @ValueSource(strings = {"22/09/2026", "01/01/2026", "31/12/2026", "29/02/2024"})
    void parsesRealDatesStrictly(String value) {
        LocalDate parsed = DateFormats.parseUserDate(value);
        assertEquals(value, DateFormats.formatUserDate(parsed));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "29/02/2025", "31/02/2026", "32/01/2026", "00/01/2026",
            "01/13/2026", "99/99/9999", "texto", "2026-09-22", ""
    })
    void rejectsInvalidDatesAndWrongFormats(String value) {
        assertThrows(DateTimeParseException.class, () -> DateFormats.parseUserDate(value));
    }

    @Test
    void convertsBetweenUserLocalDateAndSqliteRepresentations() {
        LocalDate parsed = DateFormats.parseUserDate("22/09/2026");
        assertEquals("2026-09-22", parsed.toString());
        assertEquals("22/09/2026", DateFormats.formatUserDate(LocalDate.parse(parsed.toString())));
    }
}
