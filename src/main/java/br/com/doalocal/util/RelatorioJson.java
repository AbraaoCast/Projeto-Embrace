package br.com.doalocal.util;

import br.com.doalocal.model.RelatorioEntregaItem;
import br.com.doalocal.model.RelatorioFiltro;
import br.com.doalocal.model.RelatorioOpcoes;
import br.com.doalocal.model.RelatorioQuantidade;
import br.com.doalocal.model.RelatorioResultado;

/** Serialização segura dos filtros, resultados e opções de Relatórios. */
public final class RelatorioJson {

    private RelatorioJson() {
    }

    public static String result(RelatorioResultado report) {
        return "{" +
                "\"filtro\":" + filter(report.filtro()) + "," +
                "\"resumo\":{" +
                "\"totalFamiliasCadastradas\":" + report.resumo().totalFamiliasCadastradas() + "," +
                "\"totalDoacoesCadastradas\":" + report.resumo().totalDoacoesCadastradas() + "," +
                "\"totalEntregas\":" + report.resumo().totalEntregas() + "," +
                "\"familiasAtendidas\":" + report.resumo().familiasAtendidas() +
                "}," +
                "\"quantidadesPorDoacao\":" + quantities(report) + "," +
                "\"itens\":" + items(report) +
                "}";
    }

    public static String options(RelatorioOpcoes options) {
        String families = options.familias().stream()
                .map(family -> "{\"id\":" + family.id() +
                        ",\"nomeResponsavel\":\"" + escape(family.nomeResponsavel()) + "\"}")
                .reduce("[", (json, item) -> json.length() == 1 ? json + item : json + "," + item) + "]";
        String donations = options.doacoes().stream()
                .map(donation -> "{\"id\":" + donation.id() +
                        ",\"tipo\":\"" + escape(donation.tipo()) + "\"," +
                        "\"descricao\":\"" + escape(donation.descricao()) + "\"}")
                .reduce("[", (json, item) -> json.length() == 1 ? json + item : json + "," + item) + "]";
        return "{\"familias\":" + families + ",\"doacoes\":" + donations + "}";
    }

    public static String error(String code, String message) {
        return "{\"error\":\"" + escape(code) + "\",\"message\":\"" + escape(message) + "\"}";
    }

    private static String filter(RelatorioFiltro filter) {
        return "{" +
                "\"dataInicial\":" + date(filter.dataInicial()) + "," +
                "\"dataFinal\":" + date(filter.dataFinal()) + "," +
                "\"familiaId\":" + nullableNumber(filter.familiaId()) + "," +
                "\"doacaoId\":" + nullableNumber(filter.doacaoId()) +
                "}";
    }

    private static String quantities(RelatorioResultado report) {
        return report.quantidadesPorDoacao().stream()
                .map(RelatorioJson::quantity)
                .reduce("[", (json, item) -> json.length() == 1 ? json + item : json + "," + item) + "]";
    }

    private static String quantity(RelatorioQuantidade quantity) {
        return "{" +
                "\"doacaoId\":" + quantity.doacaoId() + "," +
                "\"identificacao\":\"" + escape(quantity.identificacao()) + "\"," +
                "\"quantidade\":" + quantity.quantidade() +
                "}";
    }

    private static String items(RelatorioResultado report) {
        return report.itens().stream()
                .map(RelatorioJson::item)
                .reduce("[", (json, item) -> json.length() == 1 ? json + item : json + "," + item) + "]";
    }

    private static String item(RelatorioEntregaItem item) {
        return "{" +
                "\"entregaId\":" + item.entregaId() + "," +
                "\"dataEntrega\":\"" + DateFormats.formatUserDate(item.dataEntrega()) + "\"," +
                "\"familiaId\":" + item.familiaId() + "," +
                "\"nomeResponsavel\":\"" + escape(item.nomeResponsavel()) + "\"," +
                "\"cpfResponsavel\":\"" + escape(CpfFormats.formatForDisplay(item.cpfResponsavel())) + "\"," +
                "\"quantidadeMembros\":" + item.quantidadeMembros() + "," +
                "\"doacaoId\":" + item.doacaoId() + "," +
                "\"tipoDoacao\":\"" + escape(item.tipoDoacao()) + "\"," +
                "\"descricaoDoacao\":\"" + escape(item.descricaoDoacao()) + "\"," +
                "\"quantidadeEntregue\":" + item.quantidadeEntregue() + "," +
                "\"observacao\":" + nullable(item.observacao()) +
                "}";
    }

    private static String date(java.time.LocalDate value) {
        return value == null ? "null" : "\"" + DateFormats.formatUserDate(value) + "\"";
    }

    private static String nullableNumber(Long value) {
        return value == null ? "null" : value.toString();
    }

    private static String nullable(String value) {
        return value == null ? "null" : "\"" + escape(value) + "\"";
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
