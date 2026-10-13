package br.com.doalocal.model;

import java.time.LocalDate;

/** Visão de estoque que preserva a quantidade recebida e calcula suas movimentações. */
public record DoacaoEstoque(
        long id,
        String tipo,
        String descricao,
        int quantidadeOriginal,
        int quantidadeEntregue,
        int quantidadeDisponivel,
        LocalDate dataDoacao,
        String observacao
) { }
