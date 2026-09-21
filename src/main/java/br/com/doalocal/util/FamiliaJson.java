package br.com.doalocal.util;

import br.com.doalocal.model.Familia;

import java.time.format.DateTimeFormatter;
import java.util.List;

/** Serialização JSON mínima e segura para a API de famílias. */
public final class FamiliaJson {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private FamiliaJson() {
    }

    public static String list(List<Familia> familias) {
        return familias.stream().map(FamiliaJson::object).reduce("[", (json, item) ->
                json.length() == 1 ? json + item : json + "," + item) + "]";
    }

    public static String object(Familia familia) {
        return "{" +
                "\"id\":" + familia.getId() + "," +
                "\"cpf\":\"" + escape(familia.getCpfResponsavel()) + "\"," +
                "\"nomeResponsavel\":\"" + escape(familia.getNomeResponsavel()) + "\"," +
                "\"quantidadeMembros\":" + familia.getQuantidadeMembros() + "," +
                "\"telefone\":\"" + escape(familia.getTelefone()) + "\"," +
                "\"logradouro\":\"" + escape(familia.getLogradouro()) + "\"," +
                "\"numeroResidencia\":" + nullable(familia.getNumeroResidencia()) + "," +
                "\"bairro\":\"" + escape(familia.getBairro()) + "\"," +
                "\"cidade\":\"" + escape(familia.getCidade()) + "\"," +
                "\"dataCadastro\":\"" + DATE_TIME.format(familia.getDataCadastro()) + "\"" +
                "}";
    }

    public static String error(String code, String message) {
        return "{\"error\":\"" + escape(code) + "\",\"message\":\"" + escape(message) + "\"}";
    }

    private static String nullable(String value) {
        return value == null ? "null" : "\"" + escape(value) + "\"";
    }

    private static String escape(String value) {
        StringBuilder escaped = new StringBuilder(value.length() + 16);
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (character < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
                }
            }
        }
        return escaped.toString();
    }
}
