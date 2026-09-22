package br.com.doalocal.util;

import br.com.doalocal.model.DoacaoDisponivel;
import br.com.doalocal.model.EntregaResumo;
import br.com.doalocal.model.Familia;

import java.util.List;

/** Serialização JSON segura para histórico e opções do formulário de entregas. */
public final class EntregaJson {

    private EntregaJson() {
    }

    public static String history(List<EntregaResumo> entregas) {
        return entregas.stream().map(EntregaJson::summary).reduce("[", (json, item) ->
                json.length() == 1 ? json + item : json + "," + item) + "]";
    }

    public static String options(
            List<Familia> familias,
            List<DoacaoDisponivel> doacoes
    ) {
        String familyJson = familias.stream().map(EntregaJson::familyOption).reduce("[", (json, item) ->
                json.length() == 1 ? json + item : json + "," + item) + "]";
        String donationJson = doacoes.stream().map(EntregaJson::donationOption).reduce("[", (json, item) ->
                json.length() == 1 ? json + item : json + "," + item) + "]";
        return "{\"familias\":" + familyJson + ",\"doacoes\":" + donationJson + "}";
    }

    public static String error(String code, String message) {
        return "{\"error\":\"" + escape(code) + "\",\"message\":\"" + escape(message) + "\"}";
    }

    private static String summary(EntregaResumo entrega) {
        return "{" +
                "\"id\":" + entrega.id() + "," +
                "\"familiaId\":" + entrega.familiaId() + "," +
                "\"nomeResponsavel\":\"" + escape(entrega.nomeResponsavel()) + "\"," +
                "\"cpfResponsavel\":\"" + escape(entrega.cpfResponsavel()) + "\"," +
                "\"doacaoId\":" + entrega.doacaoId() + "," +
                "\"tipoDoacao\":\"" + escape(entrega.tipoDoacao()) + "\"," +
                "\"descricaoDoacao\":\"" + escape(entrega.descricaoDoacao()) + "\"," +
                "\"quantidadeEntregue\":" + entrega.quantidadeEntregue() + "," +
                "\"dataEntrega\":\"" + DateFormats.formatUserDate(entrega.dataEntrega()) + "\"," +
                "\"observacao\":" + nullable(entrega.observacao()) +
                "}";
    }

    private static String familyOption(Familia familia) {
        return "{" +
                "\"id\":" + familia.getId() + "," +
                "\"nomeResponsavel\":\"" + escape(familia.getNomeResponsavel()) + "\"," +
                "\"cpf\":\"" + escape(familia.getCpfResponsavel()) + "\"" +
                "}";
    }

    private static String donationOption(DoacaoDisponivel doacao) {
        return "{" +
                "\"id\":" + doacao.id() + "," +
                "\"tipo\":\"" + escape(doacao.tipo()) + "\"," +
                "\"descricao\":\"" + escape(doacao.descricao()) + "\"," +
                "\"quantidadeRecebida\":" + doacao.quantidadeRecebida() + "," +
                "\"quantidadeEntregue\":" + doacao.quantidadeEntregue() + "," +
                "\"quantidadeDisponivel\":" + doacao.quantidadeDisponivel() +
                "}";
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
