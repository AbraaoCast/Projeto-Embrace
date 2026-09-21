package br.com.doalocal.exception;

/** Impede exclusão que removeria uma entidade com histórico relacionado. */
public final class RelatedRecordsException extends RuntimeException {

    public RelatedRecordsException() {
        super("Não é possível excluir esta família porque existem registros vinculados a ela.");
    }
}
