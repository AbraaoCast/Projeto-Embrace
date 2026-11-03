package br.com.doalocal.filter;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthFilterTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "/",
            "/login-cadas.html",
            "/auth/login",
            "/auth/register",
            "/api/health",
            "/style/global.css",
            "/script/field-formats.js",
            "/img/Logo-Embrace.png"
    })
    void allowsLoginAndItsStaticResourcesWithoutAuthentication(String path) {
        assertTrue(AuthFilter.isPublic(path));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/dashboard.html",
            "/api/doacoes",
            "/gerenciar-doacoes.html",
            "/auth/logout",
            "/images/private.png"
    })
    void keepsApplicationResourcesProtected(String path) {
        assertFalse(AuthFilter.isPublic(path));
    }
}
