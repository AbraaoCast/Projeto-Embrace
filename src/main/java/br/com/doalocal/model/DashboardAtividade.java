package br.com.doalocal.model;

import java.time.LocalDate;

/** Atividade recente estruturada para apresentação no Dashboard. */
public record DashboardAtividade(
        long id,
        String tipo,
        String titulo,
        String detalhe,
        Integer quantidade,
        LocalDate data
) { }
