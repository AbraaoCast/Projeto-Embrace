package br.com.doalocal.model;

import java.time.LocalDate;

/** Filtros já validados para a consulta de entregas do relatório. */
public record RelatorioFiltro(
        LocalDate dataInicial,
        LocalDate dataFinal,
        Long familiaId,
        Long doacaoId
) { }
