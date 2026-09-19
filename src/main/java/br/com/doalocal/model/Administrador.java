package br.com.doalocal.model;

import java.util.Objects;

/** Entidade persistida do administrador. O hash nunca deve ser enviado à interface. */
public final class Administrador {

    private final long id;
    private final String nome;
    private final String email;
    private final String senhaHash;

    public Administrador(long id, String nome, String email, String senhaHash) {
        this.id = id;
        this.nome = Objects.requireNonNull(nome);
        this.email = Objects.requireNonNull(email);
        this.senhaHash = Objects.requireNonNull(senhaHash);
    }

    public long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    public String getSenhaHash() {
        return senhaHash;
    }
}
