package cz.miro.passwordvault.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import cz.miro.passwordvault.model.EncryptedPayload;

public final class CryptoUtils {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int SALT_LENGTH = 16;
    private static final int IV_LENGTH = 12;
    private static final int KEY_LENGTH = 256;
    private static final int PBKDF2_ITERATIONS = 210_000;
    private static final int GCM_TAG_LENGTH = 128;
    private static final byte[] VERIFIER_TEXT = "vault-password-check".getBytes(StandardCharsets.UTF_8);

    private CryptoUtils() {
    }

    public static byte[] newSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        SECURE_RANDOM.nextBytes(salt);
        return salt;
    }

    public static SecretKey deriveKey(char[] masterPassword, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(masterPassword, salt, PBKDF2_ITERATIONS, KEY_LENGTH);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] encoded = factory.generateSecret(spec).getEncoded();
            spec.clearPassword();
            return new SecretKeySpec(encoded, "AES");
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Nepodařilo se odvodit šifrovací klíč.", e);
        }
    }

    public static EncryptedPayload encrypt(byte[] plainBytes, SecretKey key) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            SECURE_RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] encrypted = cipher.doFinal(plainBytes);
            return new EncryptedPayload(iv, encrypted);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Šifrování selhalo.", e);
        }
    }

    public static byte[] decrypt(EncryptedPayload payload, SecretKey key) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, payload.getIv()));
            return cipher.doFinal(payload.getCipherText());
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("Neplatné hlavní heslo nebo poškozený trezor.", e);
        }
    }

    public static byte[] serialize(Serializable object) {
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
             ObjectOutputStream objectStream = new ObjectOutputStream(byteStream)) {
            objectStream.writeObject(object);
            objectStream.flush();
            return byteStream.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Nepodařilo se serializovat data trezoru.", e);
        }
    }

    public static <T> T deserialize(byte[] data, Class<T> type) {
        try (ObjectInputStream inputStream = new ObjectInputStream(new ByteArrayInputStream(data))) {
            Object object = inputStream.readObject();
            return type.cast(object);
        } catch (IOException | ClassNotFoundException | ClassCastException e) {
            throw new IllegalStateException("Nepodařilo se deserializovat data trezoru.", e);
        }
    }

    public static byte[] passwordVerifier(SecretKey key) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(key.getEncoded());
            digest.update(VERIFIER_TEXT);
            return digest.digest();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Nepodařilo se vytvořit kontrolní otisk hesla.", e);
        }
    }

    public static boolean matchesVerifier(SecretKey key, byte[] verifier) {
        return MessageDigest.isEqual(passwordVerifier(key), verifier);
    }

    public static void wipe(char[] data) {
        if (data != null) {
            Arrays.fill(data, '\0');
        }
    }

    public static void wipe(byte[] data) {
        if (data != null) {
            Arrays.fill(data, (byte) 0);
        }
    }
}
