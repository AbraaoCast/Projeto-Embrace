package br.com.doalocal.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {

    private final PasswordHasher passwordHasher = new PasswordHasher();

    @Test
    void usesRandomSaltAndVerifiesWithoutStoringPlainText() {
        String firstHash = passwordHasher.hash("Senha123");
        String secondHash = passwordHasher.hash("Senha123");

        assertNotEquals(firstHash, secondHash);
        assertFalse(firstHash.contains("Senha123"));
        assertTrue(passwordHasher.verify("Senha123", firstHash));
        assertFalse(passwordHasher.verify("SenhaErrada123", firstHash));
        assertFalse(passwordHasher.verify("Senha123", "hash-malformado"));
    }
}
