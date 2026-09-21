package br.com.doalocal.model;

import java.time.LocalDateTime;
import java.util.Objects;

/** Entidade persistida de uma família atendida pelo DoaLocal. */
public final class Familia {

    private final long id;
    private final String cpfResponsavel;
    private final String nomeResponsavel;
    private final int quantidadeMembros;
    private final String telefone;
    private final String logradouro;
    private final String numeroResidencia;
    private final String bairro;
    private final String cidade;
    private final LocalDateTime dataCadastro;

    public Familia(
            long id,
            String cpfResponsavel,
            String nomeResponsavel,
            int quantidadeMembros,
            String telefone,
            String logradouro,
            String numeroResidencia,
            String bairro,
            String cidade,
            LocalDateTime dataCadastro
    ) {
        this.id = id;
        this.cpfResponsavel = Objects.requireNonNull(cpfResponsavel);
        this.nomeResponsavel = Objects.requireNonNull(nomeResponsavel);
        this.quantidadeMembros = quantidadeMembros;
        this.telefone = Objects.requireNonNull(telefone);
        this.logradouro = Objects.requireNonNull(logradouro);
        this.numeroResidencia = numeroResidencia;
        this.bairro = Objects.requireNonNull(bairro);
        this.cidade = Objects.requireNonNull(cidade);
        this.dataCadastro = Objects.requireNonNull(dataCadastro);
    }

    public long getId() { return id; }
    public String getCpfResponsavel() { return cpfResponsavel; }
    public String getNomeResponsavel() { return nomeResponsavel; }
    public int getQuantidadeMembros() { return quantidadeMembros; }
    public String getTelefone() { return telefone; }
    public String getLogradouro() { return logradouro; }
    public String getNumeroResidencia() { return numeroResidencia; }
    public String getBairro() { return bairro; }
    public String getCidade() { return cidade; }
    public LocalDateTime getDataCadastro() { return dataCadastro; }
}
