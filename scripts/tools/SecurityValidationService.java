package com.supremeai.service;

import org.springframework.stereotype.Service;
import java.util.regex.Pattern;
import java.util.List;
import java.util.Arrays;

/**
 * Validates content for security vulnerabilities before ingestion into
 * Firestore.
 */
@Service
public class SecurityValidationService {

    private static final List<Pattern> SENSITIVE_PATTERNS = Arrays.asList(
            Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}", Pattern.CASE_INSENSITIVE), // Email
            Pattern.compile("Bearer\\s+[a-zA-Z0-9._-]+", Pattern.CASE_INSENSITIVE), // Bearer Token
            Pattern.compile("(api_key|apikey|secret|password|token)\\s*[:=]\\s*['\"][a-zA-Z0-9._%-]{10,}['\"]",
                    Pattern.CASE_INSENSITIVE), // Secrets
            Pattern.compile("AIzaSy[A-Za-z0-9_\\-]{33}") // Google API Key pattern
    );

    private static final List<String> BLACKLISTED_KEYWORDS = Arrays.asList(
            "PRIVATE KEY", "BEGIN RSA PRIVATE KEY", "id_rsa", "CLIENT_SECRET");

    public boolean isSecure(String content) {
        if (content == null || content.isBlank())
            return true;

        // Check Regex Patterns
        if (SENSITIVE_PATTERNS.stream().anyMatch(p -> p.matcher(content).find())) {
            return false;
        }

        // Check Blacklisted Keywords
        String upperContent = content.toUpperCase();
        return BLACKLISTED_KEYWORDS.stream().noneMatch(upperContent::contains);
    }
}