package org.example.validation;

public class InputValidator {
    // Validates that a string is not null and not empty
    public static boolean validateNotNullOrEmpty(String input) {
        return input != null && !input.trim().isEmpty();
    }

    // Validates the length of the string
    public static boolean validateStringLength(String input, int minLength, int maxLength) {
        return input.length() >= minLength && input.length() <= maxLength;
    }

    // Validates the input against a regex pattern
    public static boolean validatePattern(String input, String pattern) {
        return input.matches(pattern);
    }

    // Sanitizes user input by escaping special characters to prevent injection attacks
    public static String sanitizeInput(String input) {
        return input.replaceAll("[<>\"'&]", "_"); // simple sanitization
    }
}