package org.example.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EncryptionServiceTest {
    private EncryptionService encryptionService;
    
    @BeforeEach
    void setUp() {
        // Initialize with default (generated) key for testing
        encryptionService = new EncryptionService("");
    }
    
    @Test
    void testEncryptAndDecryptString() {
        String plaintext = "This is a secret message";
        
        String encrypted = encryptionService.encrypt(plaintext);
        assertNotNull(encrypted);
        assertNotEquals(plaintext, encrypted);
        
        String decrypted = encryptionService.decrypt(encrypted);
        assertEquals(plaintext, decrypted);
    }
    
    @Test
    void testEncryptEmptyString() {
        String plaintext = "";
        String encrypted = encryptionService.encrypt(plaintext);
        
        // Empty strings should pass through
        assertEquals(plaintext, encrypted);
    }
    
    @Test
    void testEncryptNullString() {
        String plaintext = null;
        String encrypted = encryptionService.encrypt(plaintext);
        
        assertNull(encrypted);
    }
    
    @Test
    void testEncryptLongString() {
        String plaintext = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. ".repeat(100);
        
        String encrypted = encryptionService.encrypt(plaintext);
        assertNotNull(encrypted);
        
        String decrypted = encryptionService.decrypt(encrypted);
        assertEquals(plaintext, decrypted);
    }
    
    @Test
    void testEncryptionProducesDifferentCiphertexts() {
        String plaintext = "Same message twice";
        
        String encrypted1 = encryptionService.encrypt(plaintext);
        String encrypted2 = encryptionService.encrypt(plaintext);
        
        // Due to random IV, ciphertexts should be different
        assertNotEquals(encrypted1, encrypted2);
        
        // But both should decrypt to same plaintext
        assertEquals(plaintext, encryptionService.decrypt(encrypted1));
        assertEquals(plaintext, encryptionService.decrypt(encrypted2));
    }
    
    @Test
    void testEncryptSpecialCharacters() {
        String plaintext = "Special chars: !@#$%^&*()_+-=[]{}|;':\",./<>?";
        
        String encrypted = encryptionService.encrypt(plaintext);
        String decrypted = encryptionService.decrypt(encrypted);
        
        assertEquals(plaintext, decrypted);
    }
    
    @Test
    void testEncryptUnicodeCharacters() {
        String plaintext = "Unicode: 你好 مرحبا Привет 🚀🔒";
        
        String encrypted = encryptionService.encrypt(plaintext);
        String decrypted = encryptionService.decrypt(encrypted);
        
        assertEquals(plaintext, decrypted);
    }
    
    @Test
    void testDecryptInvalidBase64() {
        assertThrows(RuntimeException.class, () -> {
            encryptionService.decrypt("not-valid-base64!!!!");
        });
    }
    
    @Test
    void testDecryptTamperedCiphertext() {
        String plaintext = "Original message";
        String encrypted = encryptionService.encrypt(plaintext);
        
        // Tamper with the ciphertext
        char[] chars = encrypted.toCharArray();
        if (chars.length > 5) {
            chars[chars.length - 1] = chars[chars.length - 1] == 'A' ? 'B' : 'A';
        }
        String tampered = new String(chars);
        
        // Decryption should fail due to authentication tag mismatch
        assertThrows(RuntimeException.class, () -> {
            encryptionService.decrypt(tampered);
        });
    }
    
    @Test
    void testKeyExport() {
        String exported = encryptionService.exportKeyAsString();
        assertNotNull(exported);
        assertFalse(exported.isEmpty());
        
        // Can create new service with exported key
        EncryptionService service2 = new EncryptionService(exported);
        
        String plaintext = "Test message for key reuse";
        String encrypted = encryptionService.encrypt(plaintext);
        String decrypted = service2.decrypt(encrypted);
        
        assertEquals(plaintext, decrypted);
    }
}
