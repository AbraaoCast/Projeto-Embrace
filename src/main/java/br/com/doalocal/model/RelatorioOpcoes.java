package br.com.doalocal.model;

import java.util.List;

/** Opções reais dos filtros da tela de Relatórios. */
public record RelatorioOpcoes(
        List<FamiliaOpcao> familias,
        List<DoacaoOpcao> doacoes
) {
    public record FamiliaOpcao(long id, String nomeResponsavel) { }

    public record DoacaoOpcao(long id, String tipo, String descricao) { }
}
