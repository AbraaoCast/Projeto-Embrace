package br.com.doalocal.exception;

/** Encapsula falhas técnicas de criação do PDF sem expor a biblioteca. */
public final class PdfGenerationException extends RuntimeException {

    public PdfGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
