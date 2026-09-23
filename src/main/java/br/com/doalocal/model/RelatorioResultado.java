package br.com.doalocal.model;

import java.util.List;

/** Resultado completo da consulta, pronto para JSON ou PDF. */
public record RelatorioResultado(
        RelatorioFiltro filtro,
        RelatorioResumo resumo,
        List<RelatorioQuantidade> quantidadesPorDoacao,
        List<RelatorioEntregaItem> itens
) { }
