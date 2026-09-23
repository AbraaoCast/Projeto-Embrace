package br.com.doalocal.model;

/** Quantidade distribuída agrupada por doação, sem misturar itens incompatíveis. */
public record RelatorioQuantidade(
        long doacaoId,
        String identificacao,
        int quantidade
) { }
