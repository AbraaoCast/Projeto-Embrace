package br.com.doalocal.model;

import java.time.LocalDate;

/** Linha consolidada de Entrega, Família e Doação destinada ao relatório. */
public record RelatorioEntregaItem(
        long entregaId,
        LocalDate dataEntrega,
        long familiaId,
        String nomeResponsavel,
        String cpfResponsavel,
        int quantidadeMembros,
        long doacaoId,
        String tipoDoacao,
        String descricaoDoacao,
        int quantidadeEntregue,
        String observacao
) { }
