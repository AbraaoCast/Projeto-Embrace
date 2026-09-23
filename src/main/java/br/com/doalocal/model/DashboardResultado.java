package br.com.doalocal.model;

import java.util.List;

/** Resultado agregado consumido pela interface do Dashboard. */
public record DashboardResultado(
        DashboardResumo resumo,
        List<DashboardAtividade> atividadesRecentes
) { }
