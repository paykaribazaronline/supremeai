package org.example.validation;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InputValidatorTest {
    
    @Test
    void testValidateNotNullOrEmpty() {
        assertTrue(InputValidator.validateNotNullOrEmpty("valid string"));
        assertTrue(InputValidator.validateNotNullOrEmpty("  spaces  "));
        assertFalse(InputValidator.validateNotNullOrEmpty(""));
        assertFalse(InputValidator.validateNotNullOrEmpty("   "));
        assertFalse(InputValidator.validateNotNullOrEmpty(null));
    }
    
    @Test
    void testValidateStringLength() {
        assertTrue(InputValidator.validateStringLength("hello", 1, 100));
        assertTrue(InputValidator.validateStringLength("hello", 5, 5));
        assertFalse(InputValidator.validateStringLength("hello", 6, 100));
        assertFalse(InputValidator.validateStringLength("hello", 1, 4));
    }
    
    @Test
    void testValidateStringLengthTrimmed() {
        assertTrue(InputValidator.validateStringLengthTrimmed("  hello  ", 1, 100));
        assertTrue(InputValidator.validateStringLengthTrimmed("  hello world  ", 5, 20));
        assertFalse(InputValidator.validateStringLengthTrimmed("  hello  ", 10, 100));
    }
    
    @Test
    void testValidatePattern() {
        assertTrue(InputValidator.validatePattern("abc123", "[a-z0-9]+"));
        assertFalse(InputValidator.validatePattern("ABC", "[a-z]+"));
        assertFalse(InputValidator.validatePattern(null, "[a-z]+"));
    }
    
    @Test
    void testValidateEmail() {
        assertTrue(InputValidator.validateEmail("user@example.com"));
        assertTrue(InputValidator.validateEmail("john.doe+tag@company.co.uk"));
        assertFalse(InputValidator.validateEmail("invalid.email"));
        assertFalse(InputValidator.validateEmail("@example.com"));
        assertFalse(InputValidator.validateEmail("user@"));
        assertFalse(InputValidator.validateEmail("a@b"));
        assertFalse(InputValidator.validateEmail(null));
        
        // Test length limits
        String tooLongEmail = "a".repeat(250) + "@example.com";
        assertFalse(InputValidator.validateEmail(tooLongEmail));
    }
    
    @Test
    void testValidateUrl() {
        assertTrue(InputValidator.validateUrl("https://example.com"));
        assertTrue(InputValidator.validateUrl("http://example.com:8080/path"));
        assertTrue(InputValidator.validateUrl("https://sub.example.co.uk/path/to/page"));
        assertFalse(InputValidator.validateUrl("not a url"));
        assertFalse(InputValidator.validateUrl("ftp://example.com"));
        assertFalse(InputValidator.validateUrl("javascript:alert()"));
        assertFalse(InputValidator.validateUrl(null));
    }
    
    @Test
    void testValidateAlphanumeric() {
        assertTrue(InputValidator.validateAlphanumeric("abc123"));
        assertTrue(InputValidator.validateAlphanumeric("ABC"));
        assertTrue(InputValidator.validateAlphanumeric("123"));
        assertFalse(InputValidator.validateAlphanumeric("abc-123"));
        assertFalse(InputValidator.validateAlphanumeric("abc 123"));
        assertFalse(InputValidator.validateAlphanumeric("abc_123"));
        assertFalse(InputValidator.validateAlphanumeric(null));
        assertFalse(InputValidator.validateAlphanumeric(""));
    }
    
    @Test
    void testValidateNumeric() {
        assertTrue(InputValidator.validateNumeric("123"));
        assertTrue(InputValidator.validateNumeric("0"));
        assertTrue(InputValidator.validateNumeric("9999"));
        assertFalse(InputValidator.validateNumeric("123.45"));
        assertFalse(InputValidator.validateNumeric("abc"));
        assertFalse(InputValidator.validateNumeric("-123"));
        assertFalse(InputValidator.validateNumeric(null));
        assertFalse(InputValidator.validateNumeric(""));
    }
    
    @Test
    void testValidateUUID() {
        assertTrue(InputValidator.validateUUID("550e8400-e29b-41d4-a716-446655440000"));
        assertTrue(InputValidator.validateUUID("550E8400-E29B-41D4-A716-446655440000"));
        assertFalse(InputValidator.validateUUID("not-a-uuid"));
        assertFalse(InputValidator.validateUUID("550e8400e29b41d4a716446655440000"));
        assertFalse(InputValidator.validateUUID(null));
    }
    
    @Test
    void testValidatePositiveInteger() {
        assertTrue(InputValidator.validatePositiveInteger("1"));
        assertTrue(InputValidator.validatePositiveInteger("999"));
        assertFalse(InputValidator.validatePositiveInteger("0"));
        assertFalse(InputValidator.validatePositiveInteger("-1"));
        assertFalse(InputValidator.validatePositiveInteger("abc"));
        assertFalse(InputValidator.validatePositiveInteger(null));
    }
    
    @Test
    void testValidateNonNegativeInteger() {
        assertTrue(InputValidator.validateNonNegativeInteger("0"));
        assertTrue(InputValidator.validateNonNegativeInteger("1"));
        assertTrue(InputValidator.validateNonNegativeInteger("999"));
        assertFalse(InputValidator.validateNonNegativeInteger("-1"));
        assertFalse(InputValidator.validateNonNegativeInteger("abc"));
        assertFalse(InputValidator.validateNonNegativeInteger(null));
    }
    
    @Test
    void testValidatePositiveLong() {
        assertTrue(InputValidator.validatePositiveLong("1"));
        assertTrue(InputValidator.validatePositiveLong("9999999999"));
        assertFalse(InputValidator.validatePositiveLong("0"));
        assertFalse(InputValidator.validatePositiveLong("-1"));
        assertFalse(InputValidator.validatePositiveLong("abc"));
        assertFalse(InputValidator.validatePositiveLong(null));
    }
    
    @Test
    void testIsWhitespaceOnly() {
        assertTrue(InputValidator.isWhitespaceOnly(""));
        assertTrue(InputValidator.isWhitespaceOnly("   "));
        assertTrue(InputValidator.isWhitespaceOnly("\t\n"));
        assertTrue(InputValidator.isWhitespaceOnly(null));
        assertFalse(InputValidator.isWhitespaceOnly("text"));
        assertFalse(InputValidator.isWhitespaceOnly("  text  "));
    }
    
    @Test
    void testValidateMaxLength() {
        assertTrue(InputValidator.validateMaxLength("hello", 10));
        assertTrue(InputValidator.validateMaxLength("hello", 5));
        assertFalse(InputValidator.validateMaxLength("hello", 4));
        assertTrue(InputValidator.validateMaxLength(null, 10));
    }
    
    @Test
    void testValidateStringList() {
        List<String> validList = Arrays.asList("item1", "item2", "item3");
        assertTrue(InputValidator.validateStringList(validList, 5));
        assertFalse(InputValidator.validateStringList(validList, 2));
        
        List<String> listWithEmpty = Arrays.asList("item1", "", "item3");
        assertFalse(InputValidator.validateStringList(listWithEmpty, 5));
        
        assertTrue(InputValidator.validateStringList(null, 5));
    }
}
