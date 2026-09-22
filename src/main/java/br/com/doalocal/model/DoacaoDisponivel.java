package br.com.doalocal.model;

/** Saldo calculado de uma doação sem alterar sua quantidade originalmente recebida. */
public record DoacaoDisponivel(
        long id,
        String tipo,
        String descricao,
        int quantidadeRecebida,
        int quantidadeEntregue,
        int quantidadeDisponivel
) { }
