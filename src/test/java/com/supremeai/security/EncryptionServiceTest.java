
package com.supremeai.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class EncryptionServiceTest {

    @InjectMocks
    private EncryptionService encryptionService;

    private String testPlainText;
    private String testSecretKey;

    @BeforeEach
    public void setUp() {
        testPlainText = "This is a secret message";
        testSecretKey = "test-secret-key-123";
    }

    @Test
    public void testEncrypt_Success() {
        // Act
        String encryptedText = encryptionService.encrypt(testPlainText, testSecretKey);

        // Assert
        assertNotNull(encryptedText);
        assertNotEquals(testPlainText, encryptedText);
    }

    @Test
    public void testDecrypt_Success() {
        // Arrange
        String encryptedText = encryptionService.encrypt(testPlainText, testSecretKey);

        // Act
        String decryptedText = encryptionService.decrypt(encryptedText, testSecretKey);

        // Assert
        assertEquals(testPlainText, decryptedText);
    }

    @Test
    public void testEncryptDecrypt_RoundTrip() {
        // Arrange
        String originalText = "This is a longer secret message with special characters: !@#$%^&*()";

        // Act
        String encryptedText = encryptionService.encrypt(originalText, testSecretKey);
        String decryptedText = encryptionService.decrypt(encryptedText, testSecretKey);

        // Assert
        assertEquals(originalText, decryptedText);
    }

    @Test
    public void testEncrypt_WithEmptyString() {
        // Act
        String encryptedText = encryptionService.encrypt("", testSecretKey);

        // Assert
        assertNotNull(encryptedText);
    }

    @Test
    public void testDecrypt_WithEmptyString() {
        // Arrange
        String encryptedText = encryptionService.encrypt("", testSecretKey);

        // Act
        String decryptedText = encryptionService.decrypt(encryptedText, testSecretKey);

        // Assert
        assertEquals("", decryptedText);
    }

    @Test
    public void testEncrypt_WithDifferentSecretKeys() {
        // Act
        String encrypted1 = encryptionService.encrypt(testPlainText, "secret-key-1");
        String encrypted2 = encryptionService.encrypt(testPlainText, "secret-key-2");

        // Assert
        assertNotEquals(encrypted1, encrypted2);
    }

    @Test
    public void testDecrypt_WithWrongSecretKey() {
        // Arrange
        String encryptedText = encryptionService.encrypt(testPlainText, testSecretKey);

        // Act & Assert
        assertThrows(Exception.class, () -> {
            encryptionService.decrypt(encryptedText, "wrong-secret-key");
        });
    }

    @Test
    public void testEncryptDecrypt_WithSpecialCharacters() {
        // Arrange
        String textWithSpecialChars = "Special chars: !@#$%^&*()_+-=[]{}|;:',./<>?";

        // Act
        String encryptedText = encryptionService.encrypt(textWithSpecialChars, testSecretKey);
        String decryptedText = encryptionService.decrypt(encryptedText, testSecretKey);

        // Assert
        assertEquals(textWithSpecialChars, decryptedText);
    }

    @Test
    public void testEncryptDecrypt_WithUnicode() {
        // Arrange
        String textWithUnicode = "Unicode test: 中文 日本語 한글 العربية עברית";

        // Act
        String encryptedText = encryptionService.encrypt(textWithUnicode, testSecretKey);
        String decryptedText = encryptionService.decrypt(encryptedText, testSecretKey);

        // Assert
        assertEquals(textWithUnicode, decryptedText);
    }

    @Test
    public void testEncrypt_WithVeryLongText() {
        // Arrange
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longText.append("This is line ").append(i).append(" of a very long text.\n");
        }
        String originalText = longText.toString();

        // Act
        String encryptedText = encryptionService.encrypt(originalText, testSecretKey);
        String decryptedText = encryptionService.decrypt(encryptedText, testSecretKey);

        // Assert
        assertEquals(originalText, decryptedText);
    }
}
