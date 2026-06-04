package com.supremeai.service.analysis;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Repository for security pattern rules (OWASP based). In Phase 1, patterns are hardcoded. Later
 * will be loaded from Firestore config.
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
    rules.put(
        "CRITICAL",
        List.of(
            PatternRule.builder()
                .category("SECRETS")
                .pattern(
                    Pattern.compile(
                        "(?i)(password|secret|token|api_key|apikey|access_key|aws_access_key_id|aws_secret_access_key|private_key|passphrase)\\s*=\\s*[\"'][^\"']{8,}[\"']"))
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
                .pattern(
                    Pattern.compile(
                        "(?i)aws_secret_access_key\\s*=\\s*[\"'][A-Za-z0-9/+=]{40}[\"']"))
                .message("AWS Secret Access Key exposed")
                .suggestion("Store in AWS Secrets Manager or environment variables")
                .build(),
            PatternRule.builder()
                .category("SECRETS")
                .pattern(Pattern.compile("(?i)(mongodb|mysql|postgres|redis):\\/\\/.*:.*@"))
                .message("Database credentials in connection string")
                .suggestion("Use connection pooling or environment variables for credentials")
                .build(),
            PatternRule.builder()
                .category("SECRETS")
                .pattern(Pattern.compile("\"type\"\\s*:\\s*\"service_account\""))
                .message("Firebase/GCP service account JSON embedded in code or committed file")
                .suggestion(
                    "Remove service-account.json from repo; use GOOGLE_APPLICATION_CREDENTIALS env var or Secret Manager")
                .build(),
            PatternRule.builder()
                .category("SECRETS")
                .pattern(
                    Pattern.compile(
                        "(?i)(jwt[._-]?secret|signing[._-]?key)\\s*=\\s*[\"'][^\"']{8,}[\"']"))
                .message("JWT signing secret hardcoded in source code")
                .suggestion(
                    "Load JWT secret from environment variable: System.getenv(\"JWT_SECRET\")")
                .build(),
            PatternRule.builder()
                .category("SECRETS")
                .pattern(
                    Pattern.compile(
                        "(?i)(gemini|openai|anthropic)[._-]?(api[._-]?key)\\s*=\\s*[\"'][A-Za-z0-9_\\-]{20,}[\"']"))
                .message("AI provider API key hardcoded in source")
                .suggestion("Use GCP Secret Manager or environment variables for AI API keys")
                .build()));

    // HIGH severity patterns
    rules.put(
        "HIGH",
        List.of(
            PatternRule.builder()
                .category("SQL_INJECTION")
                .pattern(
                    Pattern.compile(
                        "(executeQuery|createStatement|Statement|PreparedStatement\\(.*\\+.*\\))"))
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
                .pattern(
                    Pattern.compile(
                        "(innerHTML|document\\.write|outerHTML|insertAdjacentHTML)\\s*=\\s*[^;]+"))
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
                .pattern(Pattern.compile("(\\.\\./|\\.\\.\\\\ )"))
                .message("Path traversal pattern detected")
                .suggestion("Validate and canonicalize file paths; use whitelist approach")
                .build(),
            PatternRule.builder()
                .category("INSECURE_RANDOM")
                .pattern(Pattern.compile("(Random\\.nextInt|Math\\.random\\(\\))"))
                .message("Insecure random number generation for security-sensitive operations")
                .suggestion("Use SecureRandom for cryptographic operations")
                .build(),
            PatternRule.builder()
                .category("SPRING_SECURITY_MISCONFIGURATION")
                .pattern(Pattern.compile("(?i)requestMatchers\\(.*admin.*\\)\\.permitAll\\(\\)"))
                .message("Admin route exposed with permitAll() — authentication bypass risk")
                .suggestion("Replace with .hasRole(\"ADMIN\") for all /api/admin/** routes")
                .build(),
            PatternRule.builder()
                .category("SSRF")
                .pattern(
                    Pattern.compile(
                        "(?i)(webClient|restTemplate|HttpClient).*\\burl\\b.*request\\.getParameter"))
                .message("Potential SSRF: HTTP client URL constructed from request parameter")
                .suggestion(
                    "Whitelist allowed URLs; never pass user input directly to HTTP client URL")
                .build(),
            PatternRule.builder()
                .category("DESERIALIZATION")
                .pattern(Pattern.compile("ObjectInputStream\\s*\\("))
                .message("Java native deserialization — insecure deserialization risk")
                .suggestion(
                    "Avoid ObjectInputStream with untrusted data; use JSON/Protobuf serialization instead")
                .build(),
            PatternRule.builder()
                .category("OPEN_REDIRECT")
                .pattern(
                    Pattern.compile(
                        "(?i)(sendRedirect|RedirectView|redirect:).*request\\.getParameter"))
                .message("Potential open redirect: redirect URL from user input")
                .suggestion("Validate redirect URL against a whitelist of allowed domains")
                .build()));

    // MEDIUM severity patterns
    rules.put(
        "MEDIUM",
        List.of(
            PatternRule.builder()
                .category("WEAK_CRYPTO")
                .pattern(Pattern.compile("(MD5|SHA1|md5|sha1)\\s*\\("))
                .message("Weak hashing algorithm used")
                .suggestion("Use SHA-256 or bcrypt for password hashing")
                .build(),
            PatternRule.builder()
                .category("COMMAND_INJECTION")
                .pattern(
                    Pattern.compile("(Runtime\\.getRuntime\\(\\)\\.exec|ProcessBuilder)\\s*\\("))
                .message("Command execution detected - potential command injection")
                .suggestion("Validate and sanitize input; avoid shell execution")
                .build(),
            PatternRule.builder()
                .category("XXE")
                .pattern(
                    Pattern.compile(
                        "(DocumentBuilderFactory|XMLReader|SAXParserFactory)\\s*\\..*setFeature\\s*\\(\\s*\"http://xml.org/sax/features/external-general-entities\""))
                .message("XML external entity (XXE) vulnerability")
                .suggestion(
                    "Disable external entity processing: setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)")
                .build(),
            PatternRule.builder()
                .category("EXPOSURE")
                .pattern(
                    Pattern.compile(
                        "(?i)management\\.endpoints\\.web\\.exposure\\.include\\s*=\\s*\\*"))
                .message("All Spring Boot actuator endpoints exposed — sensitive data/control leak")
                .suggestion(
                    "Expose only needed endpoints: include=health,info; protect /actuator with .hasRole(\"ADMIN\")")
                .build(),
            PatternRule.builder()
                .category("PROTOTYPE_POLLUTION")
                .pattern(
                    Pattern.compile("__proto__|constructor\\[prototype\\]|\\.prototype\\s*\\["))
                .message("Prototype pollution pattern detected in JavaScript/TypeScript")
                .suggestion(
                    "Validate/sanitize objects from user input; use Object.create(null) for maps")
                .build(),
            PatternRule.builder()
                .category("CONFIG_EXPOSURE")
                .pattern(
                    Pattern.compile(
                        "(?i)storageBucket\\s*:\\s*\"[a-z0-9\\-]+\\.appspot\\.com\".*apiKey\\s*:\\s*\"AIza"))
                .message(
                    "Firebase client config hardcoded — verify this is intentional (client config is public)")
                .suggestion(
                    "Restrict Firebase with App Check and Firestore security rules to prevent misuse")
                .build(),
            PatternRule.builder()
                .category("CLOUD_MISCONFIGURATION")
                .pattern(Pattern.compile("--allow-unauthenticated"))
                .message("Cloud Run service deployed with --allow-unauthenticated (public access)")
                .suggestion(
                    "If backend should be private, remove flag. If intentionally public, add API-level auth (JWT)")
                .build()));

    // LOW severity patterns
    rules.put(
        "LOW",
        List.of(
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
                .build(),
            PatternRule.builder()
                .category("STUB")
                .pattern(
                    Pattern.compile(
                        "(?i)(throw new UnsupportedOperationException|return null; // TODO|not implemented)"))
                .message("Stub or unimplemented method detected")
                .suggestion("Replace stub with real implementation before deploying to production")
                .build(),
            PatternRule.builder()
                .category("ERROR_HANDLING")
                .pattern(Pattern.compile("\\.printStackTrace\\(\\)"))
                .message("printStackTrace() used — exception swallowed without proper logging")
                .suggestion("Replace with: log.error(\"Error: {}\", e.getMessage(), e)")
                .build(),
            PatternRule.builder()
                .category("ERROR_HANDLING")
                .pattern(Pattern.compile("catch\\s*\\([^)]+\\)\\s*\\{\\s*\\}"))
                .message("Empty catch block — exception silently swallowed")
                .suggestion("Add at minimum: log.warn(\"Caught exception\", e) inside catch block")
                .build(),
            PatternRule.builder()
                .category("STABILITY")
                .pattern(Pattern.compile("System\\.exit\\("))
                .message("System.exit() called — will terminate entire JVM / Cloud Run container")
                .suggestion("Throw exception instead; let Spring Boot handle graceful shutdown")
                .build()));

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
    return rulesBySeverity.values().stream().mapToInt(List::size).sum();
  }
}
