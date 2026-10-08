package security;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.Serial;
import java.io.Serializable;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;

//Chatgpt and Youtube university para magawa to hahahaha
public final class PasswordHasher {
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int DEFAULT_ITERATIONS = 210_000;
    private static final int SALT_BYTES = 16;
    private static final int HASH_BITS = 256;

    private final SecureRandom secureRandom;
    private final int iterations;

    public PasswordHasher() {
        this(DEFAULT_ITERATIONS);
    }

    public PasswordHasher(int iterations) {
        if (iterations <= 0) {
            throw new IllegalArgumentException("Iteration count must be positive.");
        }
        this.secureRandom = new SecureRandom();
        this.iterations = iterations;
    }

    public Credential hash(char[] password) {
        requirePassword(password);
        byte[] salt = new byte[SALT_BYTES];
        secureRandom.nextBytes(salt);
        byte[] hash = derive(password, salt, iterations);
        return new Credential(salt, hash, iterations);
    }

    public boolean verify(char[] password, Credential credential) {
        if (password == null || credential == null) {
            return false;
        }

        byte[] actualHash = derive(password, credential.salt, credential.iterations);
        try {
            return MessageDigest.isEqual(credential.hash, actualHash);
        } finally {
            Arrays.fill(actualHash, (byte) 0);
        }
    }

    private static byte[] derive(char[] password, byte[] salt, int iterations) {
        PBEKeySpec keySpec = new PBEKeySpec(password, salt, iterations, HASH_BITS);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(keySpec).getEncoded();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Password hashing is unavailable.", exception);
        } finally {
            keySpec.clearPassword();
        }
    }

    private static void requirePassword(char[] password) {
        if (password == null || password.length == 0) {
            throw new IllegalArgumentException("Password is required.");
        }
    }

    /** Ito yung saved password hash at salt sa employee, di yung actual password. */
    public static final class Credential implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private final byte[] salt;
        private final byte[] hash;
        private final int iterations;

        private Credential(byte[] salt, byte[] hash, int iterations) {
            this.salt = salt.clone();
            this.hash = hash.clone();
            this.iterations = iterations;
        }

        public byte[] getSalt() {
            return salt.clone();
        }

        public byte[] getHash() {
            return hash.clone();
        }

        public int getIterations() {
            return iterations;
        }
    }
}
