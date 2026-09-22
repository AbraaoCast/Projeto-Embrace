package br.com.doalocal.model;

import java.time.LocalDate;
import java.util.Objects;

/** Evento histórico que relaciona uma família a uma doação recebida. */
public final class Entrega {

    private final long id;
    private final long familiaId;
    private final long doacaoId;
    private final int quantidadeEntregue;
    private final LocalDate dataEntrega;
    private final String observacao;

    public Entrega(
            long id,
            long familiaId,
            long doacaoId,
            int quantidadeEntregue,
            LocalDate dataEntrega,
            String observacao
    ) {
        this.id = id;
        this.familiaId = familiaId;
        this.doacaoId = doacaoId;
        this.quantidadeEntregue = quantidadeEntregue;
        this.dataEntrega = Objects.requireNonNull(dataEntrega);
        this.observacao = observacao;
    }

    public long getId() { return id; }
    public long getFamiliaId() { return familiaId; }
    public long getDoacaoId() { return doacaoId; }
    public int getQuantidadeEntregue() { return quantidadeEntregue; }
    public LocalDate getDataEntrega() { return dataEntrega; }
    public String getObservacao() { return observacao; }
}
