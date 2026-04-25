
package com.supremeai.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class EncryptionServiceTest {

    @InjectMocks
    private EncryptionService encryptionService;

    private String testPlainText;

    @BeforeEach
    public void setUp() {
        testPlainText = "This is a secret message";
        String testSecretKey = EncryptionService.generateKey();
        ReflectionTestUtils.setField(encryptionService, "base64Key", testSecretKey);
    }

    @Test
    public void testEncrypt_Success() {
        String encryptedText = encryptionService.encrypt(testPlainText);

        assertNotNull(encryptedText);
        assertNotEquals(testPlainText, encryptedText);
    }

    @Test
    public void testDecrypt_Success() {
        String encryptedText = encryptionService.encrypt(testPlainText);
        String decryptedText = encryptionService.decrypt(encryptedText);

        assertEquals(testPlainText, decryptedText);
    }

    @Test
    public void testEncryptDecrypt_RoundTrip() {
        String originalText = "This is a longer secret message with special characters: !@#$%^&*()";

        String encryptedText = encryptionService.encrypt(originalText);
        String decryptedText = encryptionService.decrypt(encryptedText);

        assertEquals(originalText, decryptedText);
    }

    @Test
    public void testEncrypt_WithEmptyString() {
        String encryptedText = encryptionService.encrypt("");

        assertNotNull(encryptedText);
        assertEquals("", encryptedText);
    }

    @Test
    public void testDecrypt_WithEmptyString() {
        String decryptedText = encryptionService.decrypt("");

        assertEquals("", decryptedText);
    }

    @Test
    public void testEncrypt_WithNull() {
        String encryptedText = encryptionService.encrypt(null);

        assertNull(encryptedText);
    }

    @Test
    public void testDecrypt_WithNull() {
        String decryptedText = encryptionService.decrypt(null);

        assertNull(decryptedText);
    }

    @Test
    public void testEncrypt_WithDifferentTexts() {
        String text1 = "First message";
        String text2 = "Second message";

        String encrypted1 = encryptionService.encrypt(text1);
        String encrypted2 = encryptionService.encrypt(text2);

        assertNotEquals(encrypted1, encrypted2);
    }

    @Test
    public void testDecrypt_WithWrongCipherText() {
        assertThrows(Exception.class, () -> {
            encryptionService.decrypt("invalid-cipher-text");
        });
    }

    @Test
    public void testEncryptDecrypt_WithSpecialCharacters() {
        String textWithSpecialChars = "Special chars: !@#$%^&*()_+-=[]{}|;:',./<>?";

        String encryptedText = encryptionService.encrypt(textWithSpecialChars);
        String decryptedText = encryptionService.decrypt(encryptedText);

        assertEquals(textWithSpecialChars, decryptedText);
    }

    @Test
    public void testEncryptDecrypt_WithUnicode() {
        String textWithUnicode = "Unicode test: 中文 日本語 한글 العربية עברית";

        String encryptedText = encryptionService.encrypt(textWithUnicode);
        String decryptedText = encryptionService.decrypt(encryptedText);

        assertEquals(textWithUnicode, decryptedText);
    }

    @Test
    public void testEncrypt_WithVeryLongText() {
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longText.append("This is line ").append(i).append(" of a very long text.\n");
        }
        String originalText = longText.toString();

        String encryptedText = encryptionService.encrypt(originalText);
        String decryptedText = encryptionService.decrypt(encryptedText);

        assertEquals(originalText, decryptedText);
    }
}
