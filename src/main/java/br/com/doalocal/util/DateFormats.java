package br.com.doalocal.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;

/** Converte datas entre o formato brasileiro da interface e o tipo de domínio. */
public final class DateFormats {

    private static final DateTimeFormatter USER_DATE = DateTimeFormatter
            .ofPattern("dd/MM/uuuu")
            .withResolverStyle(ResolverStyle.STRICT);

    private DateFormats() {
    }

    public static LocalDate parseUserDate(String value) {
        return LocalDate.parse(value, USER_DATE);
    }

    public static String formatUserDate(LocalDate value) {
        return USER_DATE.format(value);
    }
}
