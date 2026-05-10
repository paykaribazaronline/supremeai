package com.supremeai.learning;

import com.supremeai.learning.immunity.CodeImmunitySystem;
import com.supremeai.learning.knowledge.SolutionMemory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ContentSanitizerService - Sanitizes scraped or submitted content before it enters
 * the knowledge base. Acts as a defensive layer against malicious or low-quality inputs.
 *
 * Responsibilities:
 * 1. Scan code snippets for known toxic patterns (via CodeImmunitySystem)
 * 2. Detect and mask PII (emails, passwords, tokens, IP addresses)
 * 3. Validate solution quality (length, security heuristics)
 * 4. Log sanitization decisions for audit
 */
@Service
public class ContentSanitizerService {

    private static final Logger log = LoggerFactory.getLogger(ContentSanitizerService.class);

    @Autowired(required = false)
    private CodeImmunitySystem immunitySystem;

    /**
     * PII detection and masking patterns.
     */
    private static final Pattern[] PII_PATTERNS = {
        // Email addresses
        Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"),
        // URL credentials: https://user:pass@example.com
        Pattern.compile("https?://[a-zA-Z0-9._%+-]+:[a-zA-Z0-9._%+-]+@"),
        // Generic API keys (long alphanumeric strings)
        Pattern.compile("(api[_-]?key|token|secret|password)\\s*[:=]\\s*['\\\"]?[a-zA-Z0-9_\\-]{20,}['\\\"]?"),
        // Credit card numbers (simplified Luhn pattern)
        Pattern.compile("\\b(?:\\d[ -]*?){13,16}\\b"),
        // IPv4 addresses
        Pattern.compile("\\b(?:[0-9]{1,3}\\.){3}[0-9]{1,3}\\b"),
        // Social Security Numbers (US)
        Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b"),
        // Phone numbers (North American)
        Pattern.compile("\\b\\(?\\d{3}\\)?[-.\\s]?\\d{3}[-.\\s]?\\d{4}\\b")
    };

    private static final String MASKED = "[REDACTED]";

    public boolean sanitizeAndValidate(SolutionMemory solution, String source) {
        if (solution == null || solution.getResolvedCode() == null) {
            log.warn("Empty solution received from {}", source);
            return false;
        }

        String originalCode = solution.getResolvedCode();
        String sanitizedCode = originalCode;

        // 0. PII Masking (always applied)
        sanitizedCode = maskPII(sanitizedCode);

        // Only update solution if PII was found and masked
        if (!sanitizedCode.equals(originalCode)) {
            solution.setResolvedCode(sanitizedCode);
            log.info("[Sanitizer] PII masked in solution from {}", source);
        }

        String code = sanitizedCode;

        // 1. Check against known toxic patterns via immunity system
        if (immunitySystem != null && immunitySystem.isCodeInfected(code)) {
            log.error("[Sanitizer] REJECTED: Solution from {} contains known toxic pattern", source);
            return false;
        }

        // 2. Basic quality heuristics
        if (code.length() > 10000) {
            log.warn("[Sanitizer] REJECTED: Solution too long ({} chars) from {}", code.length(), source);
            return false;
        }

        // 3. Check for common red flags post-masking
        if (containsHardcodedSecretsAfterMasking(code)) {
            log.warn("[Sanitizer] REJECTED: Solution contains hardcoded secrets after masking from {}", source);
            return false;
        }

        // 4. Ensure non-zero security score minimum
        if (solution.getSecurityScore() < 0.3 && !isTrustedSource(source)) {
            log.warn("[Sanitizer] REJECTED: Low security score ({}) from {}", solution.getSecurityScore(), source);
            return false;
        }

        log.info("[Sanitizer] ACCEPTED: Solution from {} passed validation", source);
        return true;
    }

    /**
     * Mask all PII patterns in text using PII_PATTERNS.
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
     * Check for hardcoded secrets (post-masking - checks keys that weren't caught by PII mask).
     * e.g., password = "something" (lowercase)
     */
    private boolean containsHardcodedSecretsAfterMasking(String code) {
        // After masking, these patterns should be gone. If they remain, it's suspicious
        String[] redFlags = {
            "password\\s*=\\s*['\\\"][^'\\\"]+['\\\"]",
            "secret\\s*=\\s*['\\\"][^'\\\"]+['\\\"]"
        };
        for (String pattern : redFlags) {
            if (code.matches("(?i).*" + pattern + ".*")) {
                return true;
            }
        }
        return false;
    }

    private boolean isTrustedSource(String source) {
        return source != null && (
            source.toLowerCase().contains("official") ||
            source.toLowerCase().contains("documentation") ||
            source.toLowerCase().contains("wikipedia")
        );
    }
}
