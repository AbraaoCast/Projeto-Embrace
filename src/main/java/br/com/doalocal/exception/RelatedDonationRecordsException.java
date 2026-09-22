package br.com.doalocal.exception;

/** Impede a exclusão de uma doação que possui entregas vinculadas. */
public final class RelatedDonationRecordsException extends RuntimeException {

    public RelatedDonationRecordsException() {
        super("Não é possível excluir esta doação porque existem entregas vinculadas a ela.");
    }
}
