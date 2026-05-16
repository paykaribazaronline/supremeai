package com.supremeai.service.analysis;

import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Repository for security pattern rules (OWASP based).
 * In Phase 1, patterns are hardcoded. Later will be loaded from Firestore config.
 */
@Component
public class PatternRepository {

    private static final Logger log = LoggerFactory.getLogger(PatternRepository.class);
    private final Map<String, List<PatternRule>> rulesBySeverity;

    public PatternRepository() {
        this.rulesBySeverity = initializeRules();
        log.info("Loaded {} security patterns", countRules());
    }

    private Map<String, List<PatternRule>> initializeRules() {
        Map<String, List<PatternRule>> rules = new HashMap<>();

        // CRITICAL severity patterns
        rules.put("CRITICAL", List.of(
            PatternRule.builder()
                .category("SECRETS")
                .pattern(Pattern.compile("(?i)(password|secret|token|api_key|apikey|access_key|aws_access_key_id|aws_secret_access_key|private_key|passphrase)\\s*=\\s*[\"'][^\"']{8,}[\"']"))
                .message("Hardcoded secret detected: {match}")
                .suggestion("Use environment variables, configuration server, or secrets manager")
                .build(),
            PatternRule.builder()
                .category("SECRETS")
                .pattern(Pattern.compile("(AKIA[0-9A-Z]{16})"))
                .message("AWS Access Key ID exposed: {match}")
                .suggestion("Remove from code, use IAM roles or environment variables")
                .build(),
            PatternRule.builder()
                .category("SECRETS")
                .pattern(Pattern.compile("(?i)aws_secret_access_key\\s*=\\s*[\"'][A-Za-z0-9/+=]{40}[\"']"))
                .message("AWS Secret Access Key exposed")
                .suggestion("Store in AWS Secrets Manager or environment variables")
                .build(),
            PatternRule.builder()
                .category("SECRETS")
                .pattern(Pattern.compile("(?i)(mongodb|mysql|postgres|redis):\\/\\/.*:.*@"))
                .message("Database credentials in connection string")
                .suggestion("Use connection pooling or environment variables for credentials")
                .build()
        ));

        // HIGH severity patterns
        rules.put("HIGH", List.of(
            PatternRule.builder()
                .category("SQL_INJECTION")
                .pattern(Pattern.compile("(executeQuery|createStatement|Statement|PreparedStatement\\(.*\\+.*\\))"))
                .message("Potential SQL injection: raw query execution with concatenation")
                .suggestion("Use parameterized queries (PreparedStatement) exclusively")
                .build(),
            PatternRule.builder()
                .category("SQL_INJECTION")
                .pattern(Pattern.compile("(SELECT|INSERT|UPDATE|DELETE).*\\+.*"))
                .message("SQL query with string concatenation detected")
                .suggestion("Use parameterized queries instead of string concatenation")
                .build(),
            PatternRule.builder()
                .category("XSS")
                .pattern(Pattern.compile("(innerHTML|document\\.write|outerHTML|insertAdjacentHTML)\\s*=\\s*[^;]+"))
                .message("Unsafe DOM manipulation - potential XSS")
                .suggestion("Use textContent or safe DOM APIs; sanitize user input")
                .build(),
            PatternRule.builder()
                .category("XSS")
                .pattern(Pattern.compile("response\\.getWriter\\(\\)\\.write\\("))
                .message("Direct response writing without encoding")
                .suggestion("Use framework encoding or OWASP Java Encoder library")
                .build(),
            PatternRule.builder()
                .category("PATH_TRAVERSAL")
                .pattern(Pattern.compile("(\\.\\./|\\.\\.\\\\)"))
                .message("Path traversal pattern detected")
                .suggestion("Validate and canonicalize file paths; use whitelist approach")
                .build(),
            PatternRule.builder()
                .category("INSECURE_RANDOM")
                .pattern(Pattern.compile("(Random\\.nextInt|Math\\.random\\(\\))"))
                .message("Insecure random number generation for security-sensitive operations")
                .suggestion("Use SecureRandom for cryptographic operations")
                .build()
        ));

        // MEDIUM severity patterns
        rules.put("MEDIUM", List.of(
            PatternRule.builder()
                .category("WEAK_CRYPTO")
                .pattern(Pattern.compile("(MD5|SHA1|md5|sha1)\\s*\\("))
                .message("Weak hashing algorithm used")
                .suggestion("Use SHA-256 or bcrypt for password hashing")
                .build(),
            PatternRule.builder()
                .category("COMMAND_INJECTION")
                .pattern(Pattern.compile("(Runtime\\.getRuntime\\(\\)\\.exec|ProcessBuilder)\\s*\\("))
                .message("Command execution detected - potential command injection")
                .suggestion("Validate and sanitize input; avoid shell execution")
                .build(),
            PatternRule.builder()
                .category("XXE")
                .pattern(Pattern.compile("(DocumentBuilderFactory|XMLReader|SAXParserFactory)\\s*\\..*setFeature\\s*\\(\\s*\"http://xml.org/sax/features/external-general-entities\""))
                .message("XML external entity (XXE) vulnerability")
                .suggestion("Disable external entity processing: setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)")
                .build()
        ));

        // LOW severity patterns
        rules.put("LOW", List.of(
            PatternRule.builder()
                .category("INFO")
                .pattern(Pattern.compile("(TODO|FIXME|XXX|HACK)\\s*:"))
                .message("Developer comment marker found")
                .suggestion("Address technical debt; remove or create ticket")
                .build(),
            PatternRule.builder()
                .category("CONFIG")
                .pattern(Pattern.compile("(debug\\s*=\\s*true|console\\.log\\()"))
                .message("Debug code or logging in production code")
                .suggestion("Remove debug statements before deployment")
                .build()
        ));

        return rules;
    }

    public Map<String, List<PatternRule>> getRulesBySeverity() {
        return rulesBySeverity;
    }

    public List<PatternRule> getRulesByCategory(String category) {
        return rulesBySeverity.values().stream()
            .flatMap(List::stream)
            .filter(rule -> rule.getCategory().equals(category))
            .collect(Collectors.toList());
    }

    private int countRules() {
        return rulesBySeverity.values().stream()
            .mapToInt(List::size)
            .sum();
    }
}
