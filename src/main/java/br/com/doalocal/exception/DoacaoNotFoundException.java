package br.com.doalocal.exception;

/** Indica que a doação solicitada não existe. */
public final class DoacaoNotFoundException extends RuntimeException {

    public DoacaoNotFoundException() {
        super("Doação não encontrada.");
    }
}
