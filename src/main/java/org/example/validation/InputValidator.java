package org.example.validation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Input Validator - Comprehensive input validation
 * Validates formats, lengths, patterns, and detects attack attempts
 */
public class InputValidator {
    private static final Logger logger = LoggerFactory.getLogger(InputValidator.class);
    
    // Common patterns
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    
    private static final Pattern URL_PATTERN = Pattern.compile(
        "^https?://[\\w\\.-]+(:\\d+)?(/[\\w\\.-]*)*/?$"
    );
    
    private static final Pattern ALPHANUMERIC_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9]+$"
    );
    
    private static final Pattern NUMERIC_PATTERN = Pattern.compile(
        "^[0-9]+$"
    );
    
    private static final Pattern UUID_PATTERN = Pattern.compile(
        "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$",
        Pattern.CASE_INSENSITIVE
    );
    
    /**
     * Validates that a string is not null and not empty
     */
    public static boolean validateNotNullOrEmpty(String input) {
        return input != null && !input.trim().isEmpty();
    }
    
    /**
     * Validates the length of a string
     */
    public static boolean validateStringLength(String input, int minLength, int maxLength) {
        if (input == null) {
            return minLength == 0;
        }
        return input.length() >= minLength && input.length() <= maxLength;
    }
    
    /**
     * Validates string length with trim
     */
    public static boolean validateStringLengthTrimmed(String input, int minLength, int maxLength) {
        if (input == null) {
            return minLength == 0;
        }
        String trimmed = input.trim();
        return trimmed.length() >= minLength && trimmed.length() <= maxLength;
    }
    
    /**
     * Validates the input against a regex pattern
     */
    public static boolean validatePattern(String input, String pattern) {
        if (input == null) {
            return false;
        }
        try {
            return input.matches(pattern);
        } catch (Exception e) {
            logger.warn("Pattern validation failed", e);
            return false;
        }
    }
    
    /**
     * Validates email format
     */
    public static boolean validateEmail(String email) {
        if (!validateStringLength(email, 5, 254)) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email).matches();
    }
    
    /**
     * Validates URL format
     */
    public static boolean validateUrl(String url) {
        if (!validateStringLength(url, 5, 2083)) {
            return false;
        }
        return URL_PATTERN.matcher(url).matches();
    }
    
    /**
     * Validates alphanumeric strings (no special characters)
     */
    public static boolean validateAlphanumeric(String input) {
        if (!validateNotNullOrEmpty(input)) {
            return false;
        }
        return ALPHANUMERIC_PATTERN.matcher(input).matches();
    }
    
    /**
     * Validates numeric strings
     */
    public static boolean validateNumeric(String input) {
        if (!validateNotNullOrEmpty(input)) {
            return false;
        }
        return NUMERIC_PATTERN.matcher(input).matches();
    }
    
    /**
     * Validates UUID format
     */
    public static boolean validateUUID(String input) {
        if (!validateNotNullOrEmpty(input)) {
            return false;
        }
        return UUID_PATTERN.matcher(input).matches();
    }
    
    /**
     * Validates positive integer
     */
    public static boolean validatePositiveInteger(String input) {
        try {
            int value = Integer.parseInt(input);
            return value > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * Validates non-negative integer
     */
    public static boolean validateNonNegativeInteger(String input) {
        try {
            int value = Integer.parseInt(input);
            return value >= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * Validates positive long
     */
    public static boolean validatePositiveLong(String input) {
        try {
            long value = Long.parseLong(input);
            return value > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * Check if string contains only whitespace
     */
    public static boolean isWhitespaceOnly(String input) {
        return input == null || input.trim().isEmpty();
    }
    
    /**
     * Validate that string doesn't exceed max length (for database fields)
     */
    public static boolean validateMaxLength(String input, int maxLength) {
        if (input == null) {
            return true;
        }
        return input.length() <= maxLength;
    }
    
    /**
     * Validate list of strings
     */
    public static boolean validateStringList(List<String> list, int maxItems) {
        if (list == null) {
            return true;
        }
        if (list.size() > maxItems) {
            return false;
        }
        return list.stream().allMatch(InputValidator::validateNotNullOrEmpty);
    }
    
    /**
     * Legacy sanitization - kept for backward compatibility
     * Prefer InputSanitizer for comprehensive sanitization
     */
    @Deprecated
    public static String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }
        return input.replaceAll("[<>\"'&]", "_");
    }
}