package br.com.doalocal.config;

/** Configuração centralizada da sessão autenticada. */
public final class AuthSession {

    public static final String ADMIN_ATTRIBUTE = "authenticatedAdministrator";
    public static final int TIMEOUT_SECONDS = 30 * 60;

    private AuthSession() {
    }
}
