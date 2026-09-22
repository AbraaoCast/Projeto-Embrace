package br.com.doalocal.util;

import br.com.doalocal.model.Doacao;

import java.util.List;

/** Serialização JSON segura e específica para a API de doações. */
public final class DoacaoJson {

    private DoacaoJson() {
    }

    public static String list(List<Doacao> doacoes) {
        return doacoes.stream().map(DoacaoJson::object).reduce("[", (json, item) ->
                json.length() == 1 ? json + item : json + "," + item) + "]";
    }

    public static String object(Doacao doacao) {
        return "{" +
                "\"id\":" + doacao.getId() + "," +
                "\"tipo\":\"" + escape(doacao.getTipo()) + "\"," +
                "\"descricao\":\"" + escape(doacao.getDescricao()) + "\"," +
                "\"quantidade\":" + doacao.getQuantidade() + "," +
                "\"dataDoacao\":\"" + DateFormats.formatUserDate(doacao.getDataDoacao()) + "\"," +
                "\"observacao\":" + nullable(doacao.getObservacao()) +
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
