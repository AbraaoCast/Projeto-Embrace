package br.com.doalocal.util;

/** Formatação visual de CPF sem alterar o valor persistido. */
public final class CpfFormats {

    private CpfFormats() {
    }

    public static String formatForDisplay(String value) {
        if (value == null || !value.matches("\\d{11}")) {
            return value;
        }
        return value.substring(0, 3) + "." + value.substring(3, 6) + "."
                + value.substring(6, 9) + "-" + value.substring(9);
    }
}
