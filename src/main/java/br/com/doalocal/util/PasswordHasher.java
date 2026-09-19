package br.com.doalocal.util;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Gera e verifica hashes PBKDF2-HMAC-SHA256 com salt aleatório. */
public final class PasswordHasher {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String FORMAT_IDENTIFIER = "pbkdf2_sha256";
    private static final int ITERATIONS = 600_000;
    private static final int SALT_BYTES = 16;
    private static final int HASH_BITS = 256;

    private final SecureRandom secureRandom;

    public PasswordHasher() {
        this.secureRandom = new SecureRandom();
    }

    public String hash(String password) {
        byte[] salt = new byte[SALT_BYTES];
        secureRandom.nextBytes(salt);
        byte[] derivedKey = derive(password.toCharArray(), salt, ITERATIONS);

        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        return String.join(
                "$",
                FORMAT_IDENTIFIER,
                Integer.toString(ITERATIONS),
                encoder.encodeToString(salt),
                encoder.encodeToString(derivedKey)
        );
    }

    public boolean verify(String password, String storedHash) {
        if (password == null || storedHash == null) {
            return false;
        }

        String[] parts = storedHash.split("\\$", -1);
        if (parts.length != 4 || !FORMAT_IDENTIFIER.equals(parts[0])) {
            return false;
        }

        try {
            int iterations = Integer.parseInt(parts[1]);
            if (iterations <= 0) {
                return false;
            }
            Base64.Decoder decoder = Base64.getUrlDecoder();
            byte[] salt = decoder.decode(parts[2]);
            byte[] expectedHash = decoder.decode(parts[3]);
            byte[] candidateHash = derive(password.toCharArray(), salt, iterations);
            return MessageDigest.isEqual(expectedHash, candidateHash);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static byte[] derive(char[] password, byte[] salt, int iterations) {
        PBEKeySpec specification = new PBEKeySpec(password, salt, iterations, HASH_BITS);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(specification).getEncoded();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Algoritmo seguro de hash indisponível.", exception);
        } finally {
            specification.clearPassword();
            java.util.Arrays.fill(password, '\0');
        }
    }
}
