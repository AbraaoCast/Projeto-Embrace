package br.com.doalocal.exception;

/** Indica que a família solicitada não existe. */
public final class FamiliaNotFoundException extends RuntimeException {

    public FamiliaNotFoundException() {
        super("Família não encontrada.");
    }
}
