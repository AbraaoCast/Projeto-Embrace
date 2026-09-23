package br.com.doalocal.model;

/** Contagens reais apresentadas nos cards do Dashboard. */
public record DashboardResumo(
        int totalFamilias,
        int totalDoacoes,
        int totalEntregas,
        int familiasAtendidas
) { }
