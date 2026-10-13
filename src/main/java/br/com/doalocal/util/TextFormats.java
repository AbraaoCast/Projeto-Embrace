package br.com.doalocal.util;

import java.text.Normalizer;
import java.util.Locale;

/** Formata nomes e endereços sem descartar caracteres válidos. */
public final class TextFormats {

    private static final Locale PORTUGUESE_BRAZIL = Locale.forLanguageTag("pt-BR");

    private TextFormats() {
    }

    public static String titleCase(String value) {
        if (value == null) {
            return null;
        }

        String normalized = Normalizer.normalize(value, Normalizer.Form.NFC)
                .toLowerCase(PORTUGUESE_BRAZIL);
        StringBuilder result = new StringBuilder(normalized.length());
        boolean wordStart = true;

        for (int offset = 0; offset < normalized.length();) {
            int codePoint = normalized.codePointAt(offset);
            if (Character.isLetter(codePoint)) {
                result.appendCodePoint(wordStart ? Character.toTitleCase(codePoint) : codePoint);
                wordStart = false;
            } else {
                result.appendCodePoint(codePoint);
                wordStart = !Character.isDigit(codePoint);
            }
            offset += Character.charCount(codePoint);
        }
        return result.toString();
    }
}
