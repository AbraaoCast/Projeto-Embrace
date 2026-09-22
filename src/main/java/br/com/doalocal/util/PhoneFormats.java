package br.com.doalocal.util;

/** Normalização e apresentação de telefone celular brasileiro. */
public final class PhoneFormats {

    private static final String ALLOWED_INPUT = "[0-9()\\s-]+";
    private static final String MOBILE_DIGITS = "[1-9][0-9]9[0-9]{8}";

    private PhoneFormats() {
    }

    public static String normalizeMobile(String value) {
        if (value == null || !value.trim().matches(ALLOWED_INPUT)) {
            return null;
        }
        String digits = value.replaceAll("\\D", "");
        return digits.matches(MOBILE_DIGITS) ? digits : null;
    }

    public static String formatForDisplay(String value) {
        if (value == null) {
            return null;
        }
        String normalized = normalizeMobile(value);
        if (normalized == null) {
            return value.trim();
        }
        return "(" + normalized.substring(0, 2) + ") "
                + normalized.substring(2, 7) + "-" + normalized.substring(7);
    }
}
