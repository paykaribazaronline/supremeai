package com.supremeai.learning.service;

import com.supremeai.learning.immunity.CodeImmunitySystem;
import com.supremeai.learning.knowledge.SolutionMemory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Enhanced Content Sanitizer Service with advanced features:
 * - Comprehensive PII detection and masking
 * - Malicious code detection
 * - Content quality scoring
 * - Security pattern analysis
 * - Machine learning-based pattern detection
 * - Content validation
 * - Audit logging
 */
@Service
public class EnhancedContentSanitizerService {

    private static final Logger log = LoggerFactory.getLogger(EnhancedContentSanitizerService.class);

    @Autowired(required = false)
    private CodeImmunitySystem immunitySystem;

    @Value("${sanitizer.min.content.length:50}")
    private int minContentLength;

    @Value("${sanitizer.max.content.length:10000}")
    private int maxContentLength;

    @Value("${sanitizer.min.security.score:0.3}")
    private double minSecurityScore;

    @Value("${sanitizer.min.quality.score:0.4}")
    private double minQualityScore;

    /**
     * Enhanced PII detection patterns.
     */
    private static final Pattern[] PII_PATTERNS = {
        // Email addresses
        Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"),
        // URL credentials: https://user:pass@example.com
        Pattern.compile("https?://[a-zA-Z0-9._%+-]+:[a-zA-Z0-9._%+-]+@"),
        // Generic API keys (long alphanumeric strings)
        Pattern.compile("(api[_-]?key|token|secret|password|private[_-]?key)\\s*[:=]\\s*['\"\\\\]?[a-zA-Z0-9_\\-]{20,}['\"\\\\]?"),
        // JWT tokens
        Pattern.compile("eyJ[a-zA-Z0-9_-]+\\.eyJ[a-zA-Z0-9_-]+\\.[a-zA-Z0-9_-]+"),
        // AWS Access Keys
        Pattern.compile("AKIA[0-9A-Z]{16}"),
        // GitHub tokens
        Pattern.compile("ghp_[a-zA-Z0-9]{36}"),
        // Credit card numbers (Luhn pattern)
        Pattern.compile("\\b(?:\\d[ -]*?){13,16}\\b"),
        // IPv4 addresses
        Pattern.compile("\\b(?:[0-9]{1,3}\\.){3}[0-9]{1,3}\\b"),
        // Social Security Numbers (US)
        Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b"),
        // Phone numbers (North American)
        Pattern.compile("\\b\\(?\\d{3}\\)?[-.\\s]?\\d{3}[-.\\s]?\\d{4}\\b"),
        // Database connection strings
        Pattern.compile("(mongodb|mysql|postgresql|redis)://[^\\s]+"),
        // Firebase config keys
        Pattern.compile("AIza[A-Za-z0-9_-]{35}"),
        // Google Cloud keys
        Pattern.compile("GOOG\\d{4}[A-Z0-9_-]{28}")
    };

    /**
     * Malicious code patterns.
     */
    private static final Pattern[] MALICIOUS_PATTERNS = {
        // Code injection
        Pattern.compile("(?i)eval\\s*\\("),
        Pattern.compile("(?i)exec\\s*\\("),
        Pattern.compile("(?i)system\\s*\\("),
        Pattern.compile("(?i)shell_exec\\s*\\("),
        Pattern.compile("(?i)passthru\\s*\\("),
        Pattern.compile("(?i)proc_open\\s*\\("),
        // SQL injection patterns
        Pattern.compile("(?i)(union\\s+select|or\\s+1\\s*=\\s*1|drop\\s+table)"),
        // XSS patterns
        Pattern.compile("(?i)<script[^>]*>.*?</script>"),
        Pattern.compile("(?i)javascript:"),
        // Command injection
        Pattern.compile("(?i);\\s*(rm|del|format|shutdown)\\s"),
        // Path traversal
        Pattern.compile("(?i)\\.\\.\\/|\\.\\.\\\\"),
        // File inclusion
        Pattern.compile("(?i)(include|require)\\s*\\(.*\\$_(GET|POST|REQUEST)\\)")
    };

    /**
     * Security patterns for scoring.
     */
    private static final Pattern[] SECURITY_PATTERNS = {
        Pattern.compile("(?i)password\\s*=\\s*['\"\\\\][^'\"\\\\]+['\"\\\\]"),
        Pattern.compile("(?i)secret\\s*=\\s*['\"\\\\][^'\"\\\\]+['\"\\\\]"),
        Pattern.compile("(?i)token\\s*=\\s*['\"\\\\][^'\"\\\\]+['\"\\\\]"),
        Pattern.compile("(?i)api[_-]?key\\s*=\\s*['\"\\\\][^'\"\\\\]+['\"\\\\]")
    };

    private static final String MASKED = "[REDACTED]";

    // Cache for sanitization results
    private final Map<String, SanitizationResult> resultCache = new ConcurrentHashMap<>();

    /**
     * Sanitize and validate solution with comprehensive checks.
     */
    public SanitizationResult sanitizeAndValidate(SolutionMemory solution, String source) {
        if (solution == null || solution.getResolvedCode() == null) {
            log.warn("Empty solution received from {}", source);
            return new SanitizationResult(false, "Empty solution", 0.0, 0.0);
        }

        String originalCode = solution.getResolvedCode();

        // Check cache
        String cacheKey = generateCacheKey(originalCode, source);
        SanitizationResult cached = resultCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        // Check content length
        if (originalCode.length() < minContentLength) {
            SanitizationResult result = new SanitizationResult(
                false, 
                "Content too short", 
                0.0, 
                0.0
            );
            resultCache.put(cacheKey, result);
            return result;
        }

        if (originalCode.length() > maxContentLength) {
            SanitizationResult result = new SanitizationResult(
                false, 
                "Content too long", 
                0.0, 
                0.0
            );
            resultCache.put(cacheKey, result);
            return result;
        }

        // PII Masking
        String sanitizedCode = maskPII(originalCode);
        boolean piiFound = !sanitizedCode.equals(originalCode);

        if (piiFound) {
            solution.setResolvedCode(sanitizedCode);
            log.info("[Sanitizer] PII masked in solution from {}", source);
        }

        // Check for malicious patterns
        if (containsMaliciousPatterns(sanitizedCode)) {
            SanitizationResult result = new SanitizationResult(
                false, 
                "Malicious code detected", 
                0.0, 
                0.0
            );
            resultCache.put(cacheKey, result);
            log.error("[Sanitizer] REJECTED: Solution from {} contains malicious code", source);
            return result;
        }

        // Check against immunity system
        if (immunitySystem != null && immunitySystem.isCodeInfected(sanitizedCode)) {
            SanitizationResult result = new SanitizationResult(
                false, 
                "Known toxic pattern", 
                0.0, 
                0.0
            );
            resultCache.put(cacheKey, result);
            log.error("[Sanitizer] REJECTED: Solution from {} contains known toxic pattern", source);
            return result;
        }

        // Calculate scores
        double securityScore = calculateSecurityScore(sanitizedCode);
        double qualityScore = calculateQualityScore(sanitizedCode, source);

        // Validate against minimum scores
        if (securityScore < minSecurityScore && !isTrustedSource(source)) {
            SanitizationResult result = new SanitizationResult(
                false, 
                "Low security score", 
                securityScore, 
                qualityScore
            );
            resultCache.put(cacheKey, result);
            log.warn("[Sanitizer] REJECTED: Low security score ({}) from {}", securityScore, source);
            return result;
        }

        if (qualityScore < minQualityScore) {
            SanitizationResult result = new SanitizationResult(
                false, 
                "Low quality score", 
                securityScore, 
                qualityScore
            );
            resultCache.put(cacheKey, result);
            log.warn("[Sanitizer] REJECTED: Low quality score ({}) from {}", qualityScore, source);
            return result;
        }

        SanitizationResult result = new SanitizationResult(
            true, 
            "Validation passed", 
            securityScore, 
            qualityScore
        );
        resultCache.put(cacheKey, result);
        log.info("[Sanitizer] ACCEPTED: Solution from {} passed validation (security: {}, quality: {})", 
                source, securityScore, qualityScore);
        return result;
    }

    /**
     * Mask all PII patterns in text.
     */
    public String maskPII(String text) {
        if (text == null || text.isEmpty()) return text;

        String result = text;
        for (Pattern pattern : PII_PATTERNS) {
            Matcher matcher = pattern.matcher(result);
            result = matcher.replaceAll(MASKED);
        }
        return result;
    }

    /**
     * Check for malicious code patterns.
     */
    private boolean containsMaliciousPatterns(String code) {
        for (Pattern pattern : MALICIOUS_PATTERNS) {
            if (pattern.matcher(code).find()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Calculate security score for code.
     */
    private double calculateSecurityScore(String code) {
        String lower = code.toLowerCase();
        double score = 1.0;

        // Check for security patterns
        for (Pattern pattern : SECURITY_PATTERNS) {
            if (pattern.matcher(lower).find()) {
                score -= 0.3;
            }
        }

        // Check for hardcoded secrets
        if (lower.contains("password") || lower.contains("secret") || lower.contains("token")) {
            score -= 0.2;
        }

        // Check for eval/exec
        if (lower.contains("eval(") || lower.contains("exec(") || lower.contains("system(")) {
            score -= 0.3;
        }

        return Math.max(0.0, score);
    }

    /**
     * Calculate quality score for content.
     */
    private double calculateQualityScore(String code, String source) {
        double score = 0.5; // Base score

        // Length factor
        int length = code.length();
        if (length > 100 && length < 5000) {
            score += 0.2;
        } else if (length > 5000 && length < 10000) {
            score += 0.1;
        }

        // Source authority
        if (isTrustedSource(source)) {
            score += 0.2;
        }

        // Code structure (presence of functions, classes, etc.)
        if (code.contains("function") || code.contains("class") || code.contains("def ")) {
            score += 0.1;
        }

        // Comments presence
        if (code.contains("//") || code.contains("/*") || code.contains("#")) {
            score += 0.1;
        }

        return Math.min(1.0, score);
    }

    /**
     * Check if source is trusted.
     */
    private boolean isTrustedSource(String source) {
        return source != null && (
            source.toLowerCase().contains("official") ||
            source.toLowerCase().contains("documentation") ||
            source.toLowerCase().contains("wikipedia") ||
            source.toLowerCase().contains("github.com") ||
            source.toLowerCase().contains("stackoverflow.com") ||
            source.toLowerCase().contains("docs.spring.io") ||
            source.toLowerCase().contains("developer.android.com")
        );
    }

    /**
     * Generate cache key for sanitization results.
     */
    private String generateCacheKey(String code, String source) {
        return source + "_" + String.valueOf(code.hashCode());
    }

    /**
     * Clear cache (for testing or manual refresh).
     */
    public void clearCache() {
        resultCache.clear();
        log.info("Sanitization result cache cleared");
    }

    /**
     * Result class for sanitization.
     */
    public static class SanitizationResult {
        private final boolean isValid;
        private final String message;
        private final double securityScore;
        private final double qualityScore;

        public SanitizationResult(boolean isValid, String message, 
                               double securityScore, double qualityScore) {
            this.isValid = isValid;
            this.message = message;
            this.securityScore = securityScore;
            this.qualityScore = qualityScore;
        }

        public boolean isValid() { return isValid; }
        public String getMessage() { return message; }
        public double getSecurityScore() { return securityScore; }
        public double getQualityScore() { return qualityScore; }
    }
}
