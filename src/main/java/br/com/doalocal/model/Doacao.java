package br.com.doalocal.model;

import java.time.LocalDate;
import java.util.Objects;

/** Entidade persistida de uma doação recebida pelo DoaLocal. */
public final class Doacao {

    private final long id;
    private final String tipo;
    private final String descricao;
    private final int quantidade;
    private final LocalDate dataDoacao;
    private final String observacao;

    public Doacao(
            long id,
            String tipo,
            String descricao,
            int quantidade,
            LocalDate dataDoacao,
            String observacao
    ) {
        this.id = id;
        this.tipo = Objects.requireNonNull(tipo);
        this.descricao = Objects.requireNonNull(descricao);
        this.quantidade = quantidade;
        this.dataDoacao = Objects.requireNonNull(dataDoacao);
        this.observacao = observacao;
    }

    public long getId() { return id; }
    public String getTipo() { return tipo; }
    public String getDescricao() { return descricao; }
    public int getQuantidade() { return quantidade; }
    public LocalDate getDataDoacao() { return dataDoacao; }
    public String getObservacao() { return observacao; }
}
