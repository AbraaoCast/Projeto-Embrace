package br.com.doalocal.model;

import java.io.Serial;
import java.io.Serializable;

/** Dados não sensíveis mantidos na sessão após a autenticação. */
public record AdministradorAutenticado(long id, String nome, String email) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public static AdministradorAutenticado from(Administrador administrador) {
        return new AdministradorAutenticado(
                administrador.getId(),
                administrador.getNome(),
                administrador.getEmail()
        );
    }
}
