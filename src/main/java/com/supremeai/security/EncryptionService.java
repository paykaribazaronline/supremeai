package com.supremeai.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM encryption service for encrypting sensitive data at rest.
 * Uses authenticated encryption (GCM) for confidentiality and integrity.
 *
 * Environment Variables:
 * - API_ENCRYPTION_KEY: Base64-encoded 32-byte (256-bit) key
 *   Generate with: openssl rand -base64 32
 */
@Service
public class EncryptionService {

    private static final Logger log = LoggerFactory.getLogger(EncryptionService.class);
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final String KEY_ALGORITHM = "AES";
    private static final int TAG_LENGTH_BIT = 128;
    private static final int IV_LENGTH_BYTE = 12;
    private static final int AES_KEY_SIZE = 256;

    @Value("${api.encryption.key:${API_ENCRYPTION_KEY:}}")
    private String base64Key;

    private SecretKey secretKey;

    private SecretKey getSecretKey() {
        if (secretKey != null) {
            return secretKey;
        }

        if (base64Key == null || base64Key.isBlank()) {
            log.warn("API_ENCRYPTION_KEY not set! Using temporary in-memory key. Set the environment variable for production.");
            secretKey = generateTempKey();
            return secretKey;
        }

        try {
            byte[] decodedKey = Base64.getDecoder().decode(base64Key.trim());
            if (decodedKey.length != 32) {
                throw new IllegalArgumentException("API_ENCRYPTION_KEY must be 32 bytes (256 bits) when decoded, got " + decodedKey.length + " bytes");
            }
            secretKey = new SecretKeySpec(decodedKey, KEY_ALGORITHM);
            log.info("EncryptionService initialized with environment-provided key");
        } catch (IllegalArgumentException e) {
            log.error("Invalid API_ENCRYPTION_KEY: {}", e.getMessage());
            throw e;
        }

        return secretKey;
    }

    private SecretKey generateTempKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance(KEY_ALGORITHM);
            keyGen.init(AES_KEY_SIZE);
            log.warn("⚠️ Using temporary encryption key - data will NOT be decryptable after restart!");
            return keyGen.generateKey();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate temporary key", e);
        }
    }

    /**
     * Encrypts plaintext using AES-256-GCM.
     * Output format: base64(iv || ciphertext)
     */
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }

        try {
            byte[] iv = new byte[IV_LENGTH_BYTE];
            SecureRandom secureRandom = new SecureRandom();
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(), parameterSpec);

            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            byte[] combined = ByteBuffer.allocate(iv.length + cipherText.length)
                    .put(iv)
                    .put(cipherText)
                    .array();

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            log.error("Encryption failed", e);
            throw new RuntimeException("Encryption failed", e);
        }
    }

    /**
     * Decrypts ciphertext (base64 format from encrypt method).
     */
    public String decrypt(String cipherText) {
        if (cipherText == null || cipherText.isEmpty()) {
            return cipherText;
        }

        try {
            byte[] decoded = Base64.getDecoder().decode(cipherText);

            if (decoded.length < IV_LENGTH_BYTE) {
                throw new IllegalArgumentException("Invalid ciphertext: too short");
            }

            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[IV_LENGTH_BYTE];
            byteBuffer.get(iv);

            byte[] cipherBytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherBytes);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), parameterSpec);

            byte[] plainText = cipher.doFinal(cipherBytes);
            return new String(plainText, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Decryption failed", e);
            throw new RuntimeException("Decryption failed", e);
        }
    }

    /**
     * Generates a base64-encoded 256-bit key for use as API_ENCRYPTION_KEY.
     * Call this method, print the result, then set it as environment variable.
     */
    public static String generateKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance(KEY_ALGORITHM);
            keyGen.init(AES_KEY_SIZE);
            SecretKey key = keyGen.generateKey();
            return Base64.getEncoder().encodeToString(key.getEncoded());
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate key", e);
        }
    }
}
