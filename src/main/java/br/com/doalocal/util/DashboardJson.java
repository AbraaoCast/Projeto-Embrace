package br.com.doalocal.util;

import br.com.doalocal.model.DashboardAtividade;
import br.com.doalocal.model.DashboardResultado;

/** Serialização segura do resumo e das atividades do Dashboard. */
public final class DashboardJson {

    private DashboardJson() {
    }

    public static String result(DashboardResultado dashboard) {
        return "{" +
                "\"resumo\":{" +
                "\"totalFamilias\":" + dashboard.resumo().totalFamilias() + "," +
                "\"totalDoacoes\":" + dashboard.resumo().totalDoacoes() + "," +
                "\"totalEntregas\":" + dashboard.resumo().totalEntregas() + "," +
                "\"familiasAtendidas\":" + dashboard.resumo().familiasAtendidas() +
                "}," +
                "\"atividadesRecentes\":" + activities(dashboard) +
                "}";
    }

    public static String error(String code, String message) {
        return "{\"error\":\"" + escape(code) + "\",\"message\":\"" + escape(message) + "\"}";
    }

    private static String activities(DashboardResultado dashboard) {
        return dashboard.atividadesRecentes().stream()
                .map(DashboardJson::activity)
                .reduce("[", (json, item) -> json.length() == 1 ? json + item : json + "," + item)
                + "]";
    }

    private static String activity(DashboardAtividade activity) {
        return "{" +
                "\"id\":" + activity.id() + "," +
                "\"tipo\":\"" + escape(activity.tipo()) + "\"," +
                "\"titulo\":\"" + escape(activity.titulo()) + "\"," +
                "\"detalhe\":" + nullable(activity.detalhe()) + "," +
                "\"quantidade\":" + nullable(activity.quantidade()) + "," +
                "\"data\":\"" + DateFormats.formatUserDate(activity.data()) + "\"" +
                "}";
    }

    private static String nullable(String value) {
        return value == null ? "null" : "\"" + escape(value) + "\"";
    }

    private static String nullable(Integer value) {
        return value == null ? "null" : value.toString();
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
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
