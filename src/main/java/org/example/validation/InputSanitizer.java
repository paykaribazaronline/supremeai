package org.example.validation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Input Sanitizer - Comprehensive sanitization for various input types
 * Prevents: XSS, SQL injection, Path traversal, Command injection
 * 
 * Note: Implements basic HTML sanitization without external library dependencies.
 * For production use, consider adding org.owasp.anti-samy library.
 */
@Service
public class InputSanitizer {
    private static final Logger logger = LoggerFactory.getLogger(InputSanitizer.class);
    
    // Dangerous HTML tags that should be completely removed
    private static final Pattern SCRIPT_TAG_PATTERN = Pattern.compile(
        "<script[^>]*>.*?</script>", 
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    private static final Pattern IFRAME_TAG_PATTERN = Pattern.compile(
        "<iframe[^>]*>.*?</iframe>",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    private static final Pattern EVENT_HANDLER_PATTERN = Pattern.compile(
        "\\s(on\\w+)\\s*=",
        Pattern.CASE_INSENSITIVE
    );
    
    // Patterns for detecting common injection attacks
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "('|\"|(\\r\\n)|(\\n)|(--)|(;)|(\\*)|(\\\\/\\*)|(\\/\\*)|(\\*\\/))",
        Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
    );
    
    private static final Pattern COMMAND_INJECTION_PATTERN = Pattern.compile(
        "([`$(){}\\[\\]|&;<>])",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern PATH_TRAVERSAL_PATTERN = Pattern.compile(
        "(\\.\\./|\\.\\.\\\\|%2e%2e|%252e)",
        Pattern.CASE_INSENSITIVE
    );
    
    /**
     * Sanitize HTML content - removes dangerous tags and handlers
     * Does NOT allow any HTML tags in this basic implementation
     * For full WYSIWYG HTML support, integrate org.owasp:antisamy library
     */
    public String sanitizeHtml(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        
        String result = input;
        
        // Remove script tags and their content
        result = SCRIPT_TAG_PATTERN.matcher(result).replaceAll("");
        
        // Remove iframe tags and their content
        result = IFRAME_TAG_PATTERN.matcher(result).replaceAll("");
        
        // Remove event handlers (onclick, onerror, onload, etc.)
        result = EVENT_HANDLER_PATTERN.matcher(result).replaceAll(" ");
        
        // Remove any remaining HTML tags
        result = result.replaceAll("<[^>]*>", "");
        
        // HTML escape the remaining text
        return escapeHtml(result);
    }
    
    /**
     * Escape HTML special characters
     */
    public String escapeHtml(String input) {
        if (input == null) {
            return null;
        }
        return input.replaceAll("&", "&amp;")
                   .replaceAll("<", "&lt;")
                   .replaceAll(">", "&gt;")
                   .replaceAll("\"", "&quot;")
                   .replaceAll("'", "&#x27;");
    }
    
    /**
     * Sanitize database query input
     * Escapes characters dangerous in SQL context
     */
    public String sanitizeSqlInput(String input) {
        if (input == null) {
            return null;
        }
        
        // Check for SQL injection patterns
        if (SQL_INJECTION_PATTERN.matcher(input).find()) {
            logger.warn("Potential SQL injection attempt detected");
            // Remove dangerous characters
            return input.replaceAll("['\";-]", "");
        }
        
        return input;
    }
    
    /**
     * Sanitize command line input
     * Prevents shell command injection
     */
    public String sanitizeCommandInput(String input) {
        if (input == null) {
            return null;
        }
        
        if (COMMAND_INJECTION_PATTERN.matcher(input).find()) {
            logger.warn("Potential command injection attempt detected");
            // Remove dangerous shell characters
            return input.replaceAll("[`$(){}\\[\\]|&;<>]", "");
        }
        
        return input;
    }
    
    /**
     * Sanitize file paths to prevent directory traversal
     */
    public String sanitizeFilePath(String path) {
        if (path == null) {
            return null;
        }
        
        if (PATH_TRAVERSAL_PATTERN.matcher(path).find()) {
            logger.warn("Potential path traversal attempt detected");
            return path.replaceAll("(\\.\\./|\\.\\.\\\\|%2e%2e|%252e)", "");
        }
        
        // Normalize path
        return path.replaceAll("\\\\+", "/")
                   .replaceAll("/+", "/");
    }
    
    /**
     * Sanitize JSON string content
     */
    public String sanitizeJson(String input) {
        if (input == null) {
            return null;
        }
        
        // Escape JSON special characters
        return input.replaceAll("\\\\", "\\\\\\\\")
                   .replaceAll("\"", "\\\\\"")
                   .replaceAll("\n", "\\\\n")
                   .replaceAll("\r", "\\\\r")
                   .replaceAll("\t", "\\\\t");
    }
    
    /**
     * Sanitize email addresses
     */
    public String sanitizeEmail(String email) {
        if (email == null) {
            return null;
        }
        
        // Check length limit (RFC 5321)
        if (email.length() > 254) {
            logger.warn("Email too long (max 254 chars)");
            return null;
        }
        
        // Basic email pattern validation
        String emailPattern = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        if (!email.matches(emailPattern)) {
            logger.warn("Invalid email format detected");
            return null;
        }
        
        return email.toLowerCase().trim();
    }
    
    /**
     * Sanitize URLs
     */
    public String sanitizeUrl(String url) {
        if (url == null) {
            return null;
        }
        
        // Only allow http(s) and relative URLs
        if (!url.matches("^(https?://|/).*") && !url.startsWith("www.")) {
            logger.warn("Invalid URL format detected");
            return null;
        }
        
        // Prevent javascript: protocol
        if (url.matches("(?i).*javascript:.*")) {
            logger.warn("JavaScript protocol in URL detected");
            return null;
        }
        
        return url;
    }
    
    /**
     * Generic sanitizer for user-provided strings
     * Applies HTML escaping and basic pattern checking
     */
    public String sanitizeString(String input, int maxLength) {
        if (input == null) {
            return null;
        }
        
        // Trim whitespace
        String sanitized = input.trim();
        
        // Enforce max length
        if (sanitized.length() > maxLength) {
            sanitized = sanitized.substring(0, maxLength);
        }
        
        // Escape HTML
        sanitized = escapeHtml(sanitized);
        
        return sanitized;
    }
    
    /**
     * Validate and sanitize user input in bulk
     */
    public Map<String, String> sanitizeMap(Map<String, String> input) {
        Map<String, String> result = new HashMap<>();
        
        for (Map.Entry<String, String> entry : input.entrySet()) {
            String key = sanitizeString(entry.getKey(), 256);
            String value = sanitizeString(entry.getValue(), 4096);
            
            if (key != null && value != null) {
                result.put(key, value);
            }
        }
        
        return result;
    }
}
