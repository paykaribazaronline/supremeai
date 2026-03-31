package org.example.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class InputSanitizerTest {
    private InputSanitizer sanitizer;
    
    @BeforeEach
    void setUp() {
        sanitizer = new InputSanitizer();
    }
    
    @Test
    void testEscapeHtml() {
        String input = "<script>alert('XSS')</script>";
        String escaped = sanitizer.escapeHtml(input);
        
        assertTrue(escaped.contains("&lt;"));
        assertTrue(escaped.contains("&gt;"));
        assertTrue(escaped.contains("&#x27;"));
        assertFalse(escaped.contains("<script>"));
    }
    
    @Test
    void testSanitizeHtml() {
        String input = "<p>Safe paragraph</p><script>alert('XSS')</script>";
        String sanitized = sanitizer.sanitizeHtml(input);
        
        assertTrue(sanitized.contains("Safe paragraph"));
        assertFalse(sanitized.contains("script"));
    }
    
    @Test
    void testEscapeHtmlSpecialChars() {
        Map<String, String> tests = Map.ofEntries(
            Map.entry("<", "&lt;"),
            Map.entry(">", "&gt;"),
            Map.entry("\"", "&quot;"),
            Map.entry("'", "&#x27;"),
            Map.entry("&", "&amp;")
        );
        
        for (var entry : tests.entrySet()) {
            String result = sanitizer.escapeHtml(entry.getKey());
            assertTrue(result.contains(entry.getValue()), 
                      "Failed to escape: " + entry.getKey());
        }
    }
    
    @Test
    void testSanitizeSqlInput() {
        String[] sqlInjections = {
            "'; DROP TABLE users; --",
            "1' OR '1'='1",
            "admin' --",
            "' UNION SELECT * FROM passwords; --"
        };
        
        for (String attack : sqlInjections) {
            String sanitized = sanitizer.sanitizeSqlInput(attack);
            // Dangerous characters should be removed
            assertFalse(sanitized.contains("'"));
            assertFalse(sanitized.contains(";"));
            assertFalse(sanitized.contains("-"));
        }
    }
    
    @Test
    void testSanitizeCommandInput() {
        String[] commandInjections = {
            "; rm -rf /",
            "| cat /etc/passwd",
            "$(whoami)",
            "`id`",
            "& tasklist"
        };
        
        for (String attack : commandInjections) {
            String sanitized = sanitizer.sanitizeCommandInput(attack);
            // Shell metacharacters should be removed
            assertFalse(sanitized.contains("|"));
            assertFalse(sanitized.contains(";"));
            assertFalse(sanitized.contains("$"));
            assertFalse(sanitized.contains("`"));
            assertFalse(sanitized.contains("&"));
        }
    }
    
    @Test
    void testSanitizeFilePath() {
        String[] pathTraversals = {
            "../../etc/passwd",
            "..\\..\\windows\\system32",
            "%2e%2e/secrets",
            "%252e%252e/config"
        };
        
        for (String attack : pathTraversals) {
            String sanitized = sanitizer.sanitizeFilePath(attack);
            assertFalse(sanitized.contains(".."));
            assertFalse(sanitized.contains("%2e%2e"));
            assertFalse(sanitized.contains("%252e"));
        }
    }
    
    @Test
    void testSanitizeJson() {
        String input = "Line1\\nLine2\\n\"quoted\"";
        String sanitized = sanitizer.sanitizeJson(input);
        
        // Should escape JSON special characters
        assertTrue(sanitized.contains("\\\\"));
        assertTrue(sanitized.contains("\\\""));
    }
    
    @Test
    void testSanitizeEmail() {
        String validEmail = "user@example.com";
        String sanitizedEmail = sanitizer.sanitizeEmail(validEmail);
        assertEquals("user@example.com", sanitizedEmail);
        
        String invalidEmail = "not-an-email";
        assertNull(sanitizer.sanitizeEmail(invalidEmail));
        
        String tooLongEmail = "a".repeat(255) + "@example.com";
        assertNull(sanitizer.sanitizeEmail(tooLongEmail));
    }
    
    @Test
    void testSanitizeUrl() {
        String validUrl = "https://example.com/path";
        assertEquals(validUrl, sanitizer.sanitizeUrl(validUrl));
        
        String javascriptUrl = "javascript:alert('XSS')";
        assertNull(sanitizer.sanitizeUrl(javascriptUrl));
        
        String relativeUrl = "/api/users";
        assertEquals(relativeUrl, sanitizer.sanitizeUrl(relativeUrl));
    }
    
    @Test
    void testSanitizeString() {
        String input = "  Normal String with <script>  ";
        String sanitized = sanitizer.sanitizeString(input, 100);
        
        assertEquals("Normal String with &lt;script&gt;", sanitized);
    }
    
    @Test
    void testSanitizeStringMaxLength() {
        String input = "This is a long string that exceeds maximum length";
        String sanitized = sanitizer.sanitizeString(input, 10);
        
        assertEquals(10, sanitized.length());
    }
    
    @Test
    void testSanitizeMap() {
        Map<String, String> input = new HashMap<>();
        input.put("name", "John <script>alert('xss')</script>");
        input.put("email", "john@example.com");
        
        Map<String, String> sanitized = sanitizer.sanitizeMap(input);
        
        assertFalse(sanitized.get("name").contains("<script>"));
        assertTrue(sanitized.get("name").contains("&lt;"));
        assertEquals("john@example.com", sanitized.get("email"));
    }
    
    @Test
    void testSanitizeNull() {
        assertNull(sanitizer.escapeHtml(null));
        assertNull(sanitizer.sanitizeSqlInput(null));
        assertNull(sanitizer.sanitizeCommandInput(null));
        assertNull(sanitizer.sanitizeFilePath(null));
        assertNull(sanitizer.sanitizeJson(null));
        assertNull(sanitizer.sanitizeEmail(null));
        assertNull(sanitizer.sanitizeUrl(null));
        assertNull(sanitizer.sanitizeString(null, 100));
    }
    
    @Test
    void testXSSPatterns() {
        String[] xssPatterns = {
            "<img src=x onerror=alert('XSS')>",
            "<svg onload=alert('XSS')>",
            "<body onload=alert('XSS')>",
            "<iframe src=javascript:alert('XSS')></iframe>",
            "<event_handler='alert(\"XSS\")'>"
        };
        
        for (String xss : xssPatterns) {
            String sanitized = sanitizer.sanitizeHtml(xss);
            // dangerous tags should be removed/escaped
            assertFalse(sanitized.toLowerCase().contains("onerror"));
            assertFalse(sanitized.toLowerCase().contains("onload"));
        }
    }
}
