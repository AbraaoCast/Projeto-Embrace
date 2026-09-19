package br.com.doalocal.exception;

/** Falha genérica de autenticação que não revela qual credencial está incorreta. */
public final class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("E-mail ou senha inválidos.");
    }
}
