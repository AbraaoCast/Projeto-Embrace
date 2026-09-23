package br.com.doalocal.model;

/** Totais reais usados nos cards e no cabeçalho do PDF. */
public record RelatorioResumo(
        int totalFamiliasCadastradas,
        int totalDoacoesCadastradas,
        int totalEntregas,
        int familiasAtendidas
) { }
