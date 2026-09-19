package br.com.doalocal.exception;

/** Indica conflito esperado com a restrição UNIQUE do e-mail. */
public final class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException() {
        super("Já existe uma conta cadastrada com este e-mail.");
    }
}
