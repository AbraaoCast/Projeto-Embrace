package br.com.doalocal.model;

import java.time.LocalDate;

/** Dados humanos necessários para apresentar uma entrega no histórico. */
public record EntregaResumo(
        long id,
        long familiaId,
        String nomeResponsavel,
        String cpfResponsavel,
        long doacaoId,
        String tipoDoacao,
        String descricaoDoacao,
        int quantidadeEntregue,
        LocalDate dataEntrega,
        String observacao
) { }
