package br.com.doalocal.exception;

/** Indica que o CPF informado já pertence a outra família. */
public final class DuplicateCpfException extends RuntimeException {

    public DuplicateCpfException() {
        super("Já existe uma família cadastrada com este CPF.");
    }
}
