package br.com.doalocal.exception;

/** Encapsula falhas técnicas da persistência sem expor detalhes JDBC às outras camadas. */
public final class DataAccessException extends RuntimeException {

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
