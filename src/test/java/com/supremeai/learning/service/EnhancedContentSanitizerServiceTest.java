package com.supremeai.learning.service;

import com.supremeai.learning.immunity.CodeImmunitySystem;
import com.supremeai.learning.knowledge.SolutionMemory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EnhancedContentSanitizerService.
 * Tests comprehensive PII detection, malicious code scanning, quality scoring.
 */
class EnhancedContentSanitizerServiceTest {EnhancedContentSanitizerServicepublic EnhancedContentSanitizerServiceTest(EnhancedContentSanitizerService sanitizer, CodeImmunitySystem mockImmunity) {
EnhancedContentSanitizerService    this.sanitizer = sanitizer;
EnhancedContentSanitizerService    this.mockImmunity = mockImmunity;
EnhancedContentSanitizerService}






    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        sanitizer = new EnhancedContentSanitizerService();
        sanitizer.clearCache();
        
        // Inject values
        ReflectionTestUtils.setField(sanitizer, "minContentLength", 50);
        ReflectionTestUtils.setField(sanitizer, "maxContentLength", 10000);
        ReflectionTestUtils.setField(sanitizer, "minSecurityScore", 0.3);
        ReflectionTestUtils.setField(sanitizer, "minQualityScore", 0.4);
        
        // Inject mock immunity
        ReflectionTestUtils.setField(sanitizer, "immunitySystem", mockImmunity);
    }

    @Test
    void testSanitizeAndValidate_nullSolution_returnsInvalid() {
        EnhancedContentSanitizerService.SanitizationResult result = sanitizer.sanitizeAndValidate(null, "test");
        assertFalse(result.isValid());
        assertTrue(result.getMessage().contains("Empty"));
    }

    @Test
    void testSanitizeAndValidate_contentTooShort_rejected() {
        SolutionMemory mem = new SolutionMemory("err", "short", "src", 100L, 0.8);
        EnhancedContentSanitizerService.SanitizationResult result = sanitizer.sanitizeAndValidate(mem, "test");
        assertFalse(result.isValid());
        assertTrue(result.getMessage().contains("too short"));
    }

    @Test
    void testSanitizeAndValidate_contentTooLong_rejected() {
        String longCode = "x".repeat(11000);
        SolutionMemory mem = new SolutionMemory("err", longCode, "src", 100L, 0.8);
        EnhancedContentSanitizerService.SanitizationResult result = sanitizer.sanitizeAndValidate(mem, "test");
        assertFalse(result.isValid());
        assertTrue(result.getMessage().contains("too long"));
    }

    @Test
    void testSanitizeAndValidate_maliciousPattern_detected() {
        String code = "eval(user_input); more content here to make it longer";
        SolutionMemory mem = new SolutionMemory("err", code, "src", 100L, 0.8);
        EnhancedContentSanitizerService.SanitizationResult result = sanitizer.sanitizeAndValidate(mem, "test");
        assertFalse(result.isValid());
        assertTrue(result.getMessage().contains("Malicious") || result.getMessage().contains("malicious"));
    }

    @Test
    void testSanitizeAndValidate_sqlInjectionDetected() {
        String code = "query = \"SELECT * FROM users WHERE id = 1 OR 1=1\"; // SQL injection";
        SolutionMemory mem = new SolutionMemory("err", code, "src", 100L, 0.8);
        EnhancedContentSanitizerService.SanitizationResult result = sanitizer.sanitizeAndValidate(mem, "test");
        assertFalse(result.isValid());
    }

    @Test
    void testSanitizeAndValidate_xssDetected() {
        String code = "document.write(\"<script>alert('xss')</script>\");";
        SolutionMemory mem = new SolutionMemory("err", code, "src", 100L, 0.8);
        EnhancedContentSanitizerService.SanitizationResult result = sanitizer.sanitizeAndValidate(mem, "test");
        assertFalse(result.isValid());
    }

    @Test
    void testSanitizeAndValidate_knownToxicPatternFromImmunity_rejected() {
        when(mockImmunity.isCodeInfected(anyString())).thenReturn(true);
        String code = "some safe looking code with enough length to pass all the other checks easily";
        SolutionMemory mem = new SolutionMemory("err", code, "src", 100L, 0.8);
        EnhancedContentSanitizerService.SanitizationResult result = sanitizer.sanitizeAndValidate(mem, "test");
        assertFalse(result.isValid());
        assertEquals("Known toxic pattern", result.getMessage());
    }

    @Test
    void testSanitizeAndValidate_lowSecurityScoreFromUntrustedSource_rejected() {
        when(mockImmunity.isCodeInfected(anyString())).thenReturn(false);
        String code = "password = 'hardcoded123'; eval(userInput); system(cmd);";
        SolutionMemory mem = new SolutionMemory("err", code, "random-site.com", 100L, 0.2);
        EnhancedContentSanitizerService.SanitizationResult result = sanitizer.sanitizeAndValidate(mem, "random-site.com");
        // Code with malicious patterns (eval, system) should be rejected as malicious
        assertFalse(result.isValid());
    }

    @Test
    void testSanitizeAndValidate_lowQualityScore_rejected() {
        String shortCode = "x=1; y=2; z=3; a=4; b=5; c=6; d=7; e=8; f=9; g=10";
        SolutionMemory mem = new SolutionMemory("err", shortCode, "random.com", 100L, 0.9);
        EnhancedContentSanitizerService.SanitizationResult result = sanitizer.sanitizeAndValidate(mem, "random.com");
        assertFalse(result.isValid());
    }

    @Test
    void testSanitizeAndValidate_validAllChecksPass() {
        String goodCode = "public class Hello { public static void main(String[] args) { System.out.println(\"Hello\"); } }";
        SolutionMemory mem = new SolutionMemory("err", goodCode, "github.com", 100L, 0.9);
        EnhancedContentSanitizerService.SanitizationResult result = sanitizer.sanitizeAndValidate(mem, "github.com");
        assertTrue(result.isValid());
        assertTrue(result.getSecurityScore() > 0);
        assertTrue(result.getQualityScore() > 0);
    }

    @Test
    void testMaskPII_email() {
        String input = "Contact: admin@example.com";
        String masked = sanitizer.maskPII(input);
        assertFalse(masked.contains("admin@example.com"));
        assertTrue(masked.contains("[REDACTED]"));
    }

    @Test
    void testMaskPII_jwtToken() {
        String jwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
        String masked = sanitizer.maskPII(jwt);
        assertFalse(masked.contains("eyJ"));
        assertTrue(masked.contains("[REDACTED]"));
    }

    @Test
    void testMaskPII_awsAccessKey() {
        String input = "AWS_ACCESS_KEY=AKIAIOSFODNN7EXAMPLE";
        String masked = sanitizer.maskPII(input);
        assertFalse(masked.contains("AKIA"));
        assertTrue(masked.contains("[REDACTED]"));
    }

    @Test
    void testMaskPII_githubToken() {
        String input = "ghp_1234567890abcdefghijklmnopqrstuvwxyz";
        String masked = sanitizer.maskPII(input);
        assertFalse(masked.contains("ghp_"));
        assertTrue(masked.contains("[REDACTED]"));
    }

    @Test
    void testMaskPII_creditCard() {
        String input = "Card: 4532-1234-5678-9010";
        String masked = sanitizer.maskPII(input);
        assertFalse(masked.contains("4532"));
        assertTrue(masked.contains("[REDACTED]"));
    }

    @Test
    void testMaskPII_multiplePIIs() {
        String input = "Email: test@test.com, Token: ghp_1234567890123456789012345678901234567890, AWS: AKIAIOSFODNN7EXAMPLE12345";
        String masked = sanitizer.maskPII(input);
        assertFalse(masked.contains("test@test.com"));
        assertFalse(masked.contains("ghp_"));
        assertFalse(masked.contains("AKIAIOSFODNN7EXAMPLE"));
        assertTrue(masked.contains("[REDACTED]"));
    }

    @Test
    void testIsTrustedSource_recognizesOfficialDomains() {
        assertTrue((boolean) ReflectionTestUtils.invokeMethod(sanitizer, "isTrustedSource", "Official Documentation"));
        assertTrue((boolean) ReflectionTestUtils.invokeMethod(sanitizer, "isTrustedSource", "wikipedia.org"));
        assertTrue((boolean) ReflectionTestUtils.invokeMethod(sanitizer, "isTrustedSource", "github.com"));
        assertTrue((boolean) ReflectionTestUtils.invokeMethod(sanitizer, "isTrustedSource", "stackoverflow.com"));
        assertTrue((boolean) ReflectionTestUtils.invokeMethod(sanitizer, "isTrustedSource", "docs.spring.io"));
        assertFalse((boolean) ReflectionTestUtils.invokeMethod(sanitizer, "isTrustedSource", "random-blog.com"));
    }

    @Test
    void testContainsMaliciousPatterns_detectsEval() {
        String code = "eval('malicious')";
        assertTrue((boolean) ReflectionTestUtils.invokeMethod(sanitizer, "containsMaliciousPatterns", code));
    }

    @Test
    void testContainsMaliciousPatterns_detectsSqlInjection() {
        String code = "sql = \"SELECT * FROM users WHERE id=\" + userId + \" OR 1=1\"";
        assertTrue((boolean) ReflectionTestUtils.invokeMethod(sanitizer, "containsMaliciousPatterns", code));
    }

    @Test
    void testContainsMaliciousPatterns_detectsXss() {
        String code = "out.println(\"<script>alert('x')</script>\");";
        assertTrue((boolean) ReflectionTestUtils.invokeMethod(sanitizer, "containsMaliciousPatterns", code));
    }

    @Test
    void testContainsMaliciousPatterns_detectsPathTraversal() {
        String code = "File f = new File(\"../../../etc/passwd\");";
        assertTrue((boolean) ReflectionTestUtils.invokeMethod(sanitizer, "containsMaliciousPatterns", code));
    }

    @Test
    void testContainsMaliciousPatterns_cleanCodeReturnsFalse() {
        String code = "System.out.println(\"Hello\");";
        assertFalse((boolean) ReflectionTestUtils.invokeMethod(sanitizer, "containsMaliciousPatterns", code));
    }

    @Test
    void testCalculateSecurityScore_highSecurityCode() {
        String code = "public class SecureClass { }";
        double score = invokeCalculateSecurityScore(code);
        assertTrue(score > 0.8);
    }

    @Test
    void testCalculateSecurityScore_lowSecurity() {
        String code = "password = 'secret123'; eval(input);";
        double score = invokeCalculateSecurityScore(code);
        assertTrue(score < 0.5);
    }

    @Test
    void testCalculateQualityScore_wellStructuredCode() {
        String code = "/**\n" +
                " * This function calculates the sum.\n" +
                " */\n" +
                "public int sum(int a, int b) {\n" +
                "    return a + b;\n" +
                "}";
        double score = invokeCalculateQualityScore(code, "github.com");
        assertTrue(score > 0.7);
    }

    @Test
    void testSanitizationResult_getters() {
        EnhancedContentSanitizerService.SanitizationResult result =
            new EnhancedContentSanitizerService.SanitizationResult(true, "OK", 0.9, 0.8);
        assertTrue(result.isValid());
        assertEquals("OK", result.getMessage());
        assertEquals(0.9, result.getSecurityScore(), 0.001);
        assertEquals(0.8, result.getQualityScore(), 0.001);
    }

    // Private method accessors
    private double invokeCalculateSecurityScore(String code) {
        try {
            var method = EnhancedContentSanitizerService.class.getDeclaredMethod("calculateSecurityScore", String.class);
            method.setAccessible(true);
            return (double) method.invoke(sanitizer, code);
        } catch (Exception e) {
            fail("Failed: " + e.getMessage());
            return 0.0;
        }
    }

    private double invokeCalculateQualityScore(String code, String source) {
        try {
            var method = EnhancedContentSanitizerService.class.getDeclaredMethod("calculateQualityScore", String.class, String.class);
            method.setAccessible(true);
            return (double) method.invoke(sanitizer, code, source);
        } catch (Exception e) {
            fail("Failed: " + e.getMessage());
            return 0.0;
        }
    }
}
