package org.example.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Encryption Service for AES-256-GCM
 * Provides data encryption/decryption with authenticated encryption
 * Protects sensitive data at rest
 */
@Service
public class EncryptionService {
    private static final Logger logger = LoggerFactory.getLogger(EncryptionService.class);
    
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int KEY_SIZE = 256; // AES-256
    private static final int GCM_IV_LENGTH = 12; // 96 bits
    private static final int GCM_TAG_LENGTH = 128; // 128 bits
    
    private final SecretKey encryptionKey;
    private final SecureRandom secureRandom;
    
    public EncryptionService(@Value("${supremeai.encryption.key:}") String keyString) {
        this.secureRandom = new SecureRandom();
        
        try {
            if (keyString != null && !keyString.isEmpty()) {
                // Load key from configuration
                this.encryptionKey = loadKeyFromString(keyString);
                logger.info("Encryption key loaded from configuration");
            } else {
                // Generate new key if not provided
                this.encryptionKey = generateKey();
                logger.warn("Generated new encryption key - consider storing in secure configuration");
            }
        } catch (Exception e) {
            logger.error("Failed to initialize encryption key", e);
            throw new RuntimeException("Encryption service initialization failed", e);
        }
    }
    
    /**
     * Encrypt plaintext with AES-256-GCM
     * @param plaintext Data to encrypt
     * @return Base64-encoded ciphertext with IV
     */
    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            return plaintext;
        }
        
        try {
            byte[] plainBytes = plaintext.getBytes(StandardCharsets.UTF_8);
            
            // Generate random IV (Initialization Vector)
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);
            
            // Initialize cipher
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, gcmSpec);
            
            // Encrypt plaintext
            byte[] ciphertext = cipher.doFinal(plainBytes);
            
            // Combine IV + ciphertext for storage/transmission
            ByteBuffer buffer = ByteBuffer.allocate(GCM_IV_LENGTH + ciphertext.length);
            buffer.put(iv);
            buffer.put(ciphertext);
            
            // Encode as Base64 for safe transmission
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception e) {
            logger.error("Encryption failed", e);
            throw new RuntimeException("Encryption failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Decrypt Base64-encoded ciphertext with AES-256-GCM
     * @param encryptedBase64 Base64-encoded data (IV + ciphertext)
     * @return Decrypted plaintext
     */
    public String decrypt(String encryptedBase64) {
        if (encryptedBase64 == null || encryptedBase64.isEmpty()) {
            return encryptedBase64;
        }
        
        try {
            // Decode Base64
            byte[] encryptedData = Base64.getDecoder().decode(encryptedBase64);
            
            // Extract IV and ciphertext
            ByteBuffer buffer = ByteBuffer.wrap(encryptedData);
            byte[] iv = new byte[GCM_IV_LENGTH];
            buffer.get(iv);
            
            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);
            
            // Initialize cipher for decryption
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey, gcmSpec);
            
            // Decrypt
            byte[] plainBytes = cipher.doFinal(ciphertext);
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.error("Decryption failed", e);
            throw new RuntimeException("Decryption failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Generate a new AES-256 key
     */
    private SecretKey generateKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(KEY_SIZE, secureRandom);
        return keyGen.generateKey();
    }
    
    /**
     * Load key from Base64-encoded string
     */
    private SecretKey loadKeyFromString(String keyString) throws Exception {
        byte[] decodedKey = Base64.getDecoder().decode(keyString);
        return new javax.crypto.spec.SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
    }
    
    /**
     * Export key as Base64 string for configuration
     * WARNING: Only use for secure key storage
     */
    public String exportKeyAsString() {
        return Base64.getEncoder().encodeToString(encryptionKey.getEncoded());
    }
}
