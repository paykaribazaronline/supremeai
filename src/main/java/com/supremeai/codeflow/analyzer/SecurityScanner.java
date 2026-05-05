package com.supremeai.codeflow.analyzer;

import com.supremeai.codeflow.model.CodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Security vulnerability scanner
 * Detects hardcoded secrets, SQL injection, XSS, debug statements, etc.
 */
@Component
public class SecurityScanner {
    
    private static final Logger logger = LoggerFactory.getLogger(SecurityScanner.class);
    
    // Security patterns to detect
    private static final List<SecurityPattern> SECURITY_PATTERNS = List.of(
        // Hardcoded secrets
        new SecurityPattern(
            "HARDCODED_SECRET",
            "CRITICAL",
            Pattern.compile("(?i)(password|passwd|pwd|secret|api_key|apikey|token|auth)\\s*[=:]\\s*['\"][^'\"]{8,}['\"]"),
            "Hardcoded secret detected",
            "Use environment variables or secure vault for secrets",
            "CWE-798"
        ),
        new SecurityPattern(
            "AWS_SECRET",
            "CRITICAL",
            Pattern.compile("AKIA[0-9A-Z]{16}"),
            "AWS access key ID detected",
            "Remove AWS credentials from code",
            "CWE-798"
        ),
        new SecurityPattern(
            "PRIVATE_KEY",
            "CRITICAL",
            Pattern.compile("(?i)-----BEGIN (RSA |EC |DSA |OPENSSH )?PRIVATE KEY-----"),
            "Private key detected",
            "Remove private keys from code repository",
            "CWE-798"
        ),
        
        // SQL Injection
        new SecurityPattern(
            "SQL_INJECTION",
            "HIGH",
            Pattern.compile("(?i)(execute|query|exec)\\s*\\([^)]*['\"].*\\+.*['\"]"),
            "Potential SQL injection via string concatenation",
            "Use parameterized queries or prepared statements",
            "CWE-89"
        ),
        new SecurityPattern(
            "SQL_INJECTION_CONCAT",
            "HIGH",
            Pattern.compile("(?i)(SELECT|INSERT|UPDATE|DELETE).*['\"].*\\+.*['\"]"),
            "SQL query with string concatenation",
            "Use parameterized queries",
            "CWE-89"
        ),
        
        // XSS
        new SecurityPattern(
            "XSS_HTML",
            "HIGH",
            Pattern.compile("(?i)(innerHTML|outerHTML|document\\.write)\\s*="),
            "Potential XSS via direct HTML injection",
            "Use textContent or sanitize HTML input",
            "CWE-79"
        ),
        new SecurityPattern(
            "XSS_EVAL",
            "CRITICAL",
            Pattern.compile("\\beval\\s*\\("),
            "Use of eval() - code injection risk",
            "Avoid eval(), use safer alternatives",
            "CWE-95"
        ),
        
        // Command Injection
        new SecurityPattern(
            "CMD_INJECTION",
            "CRITICAL",
            Pattern.compile("(?i)(exec|system|popen|Runtime\\.getRuntime\\(\\)\\.exec)\\s*\\([^)]*['\"].*\\+"),
            "Potential command injection",
            "Validate and sanitize all inputs, use parameterized commands",
            "CWE-78"
        ),
        
        // Path Traversal
        new SecurityPattern(
            "PATH_TRAVERSAL",
            "HIGH",
            Pattern.compile("(?i)(File|Path).*[\"'].*\\.\\."),
            "Potential path traversal vulnerability",
            "Validate file paths, use canonical paths",
            "CWE-22"
        ),
        
        // Debug statements
        new SecurityPattern(
            "DEBUG_STATEMENT",
            "LOW",
            Pattern.compile("console\\.(log|debug|trace)|System\\.out\\.print"),
            "Debug statement in production code",
            "Remove or use proper logging framework",
            "CWE-489"
        ),
        
        // Hardcoded IPs
        new SecurityPattern(
            "HARDCODED_IP",
            "MEDIUM",
            Pattern.compile("\\b(?:[0-9]{1,3}\\.){3}[0-9]{1,3}\\b"),
            "Hardcoded IP address",
            "Use configuration files or DNS",
            "CWE-798"
        ),
        
        // Weak crypto
        new SecurityPattern(
            "WEAK_CRYPTO",
            "HIGH",
            Pattern.compile("(?i)(MD5|SHA1|DES|RC4)\\b"),
            "Weak cryptographic algorithm",
            "Use SHA-256, SHA-3, or AES",
            "CWE-327"
        ),
        
        // Insecure random
        new SecurityPattern(
            "INSECURE_RANDOM",
            "MEDIUM",
            Pattern.compile("\\bMath\\.random\\s*\\(\\)"),
            "Insecure random number generation",
            "Use SecureRandom for security-sensitive operations",
            "CWE-330"
        ),
        
        // Missing auth
        new SecurityPattern(
            "MISSING_AUTH",
            "HIGH",
            Pattern.compile("(?i)(@(GetMapping|PostMapping|PutMapping|DeleteMapping).*public)"),
            "Public endpoint without explicit security",
            "Add authentication/authorization checks",
            "CWE-306"
        )
    );
    
    /**
     * Scan files for security vulnerabilities
     */
    public List<CodeRepository.SecurityIssue> scan(List<CodeRepository.CodeFile> files) {
        List<CodeRepository.SecurityIssue> issues = new ArrayList<>();
        
        for (CodeRepository.CodeFile file : files) {
            issues.addAll(scanFile(file));
        }
        
        logger.info("Security scan found {} issues across {} files", 
            issues.size(), files.size());
        
        return issues;
    }
    
    /**
     * Scan individual file
     */
    private List<CodeRepository.SecurityIssue> scanFile(CodeRepository.CodeFile file) {
        List<CodeRepository.SecurityIssue> issues = new ArrayList<>();
        
        try {
            String content = readFileContent(file);
            if (content == null || content.isEmpty()) {
                return issues;
            }
            
            String[] lines = content.split("\\n");
            
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                int lineNumber = i + 1;
                
                for (SecurityPattern pattern : SECURITY_PATTERNS) {
                    Matcher matcher = pattern.getPattern().matcher(line);
                    if (matcher.find()) {
                        // Skip false positives
                        if (isFalsePositive(line, pattern)) {
                            continue;
                        }
                        
                        issues.add(createSecurityIssue(pattern, file, lineNumber, line));
                    }
                }
                
                // Additional context-aware checks
                issues.addAll(checkContextVulnerabilities(file, line, lineNumber, lines));
            }
            
        } catch (Exception e) {
            logger.warn("Failed to scan file: " + file.getPath(), e);
        }
        
        return issues;
    }
    
    /**
     * Read file content (simulated - in production would read from storage)
     */
    private String readFileContent(CodeRepository.CodeFile file) {
        // In production, this would fetch actual file content
        // For now, return empty string as content is not stored
        return "";
    }
    
    /**
     * Check for context-aware vulnerabilities
     */
    private List<CodeRepository.SecurityIssue> checkContextVulnerabilities(
            CodeRepository.CodeFile file, String line, int lineNumber, String[] allLines) {
        List<CodeRepository.SecurityIssue> issues = new ArrayList<>();
        
        // Check for missing @Transactional on database operations
        if (line.contains("@Repository") || line.contains("EntityManager")) {
            boolean hasTransactional = false;
            for (int i = Math.max(0, lineNumber - 5); i < Math.min(allLines.length, lineNumber + 5); i++) {
                if (allLines[i].contains("@Transactional")) {
                    hasTransactional = true;
                    break;
                }
            }
            if (!hasTransactional && line.contains("public")) {
                issues.add(createSecurityIssue(
                    new SecurityPattern(
                        "MISSING_TRANSACTIONAL",
                        "MEDIUM",
                        Pattern.compile(".*"),
                        "Repository method without @Transactional",
                        "Add @Transactional for database operations",
                        "CWE-667"
                    ),
                    file,
                    lineNumber,
                    line
                ));
            }
        }
        
        // Check for exposed sensitive data in logs
        if (line.contains("log") && (line.contains("password") || line.contains("secret"))) {
            issues.add(createSecurityIssue(
                new SecurityPattern(
                    "SENSITIVE_LOG",
                    "HIGH",
                    Pattern.compile(".*"),
                    "Sensitive data in logs",
                    "Never log passwords or secrets",
                    "CWE-532"
                ),
                file,
                lineNumber,
                line
            ));
        }
        
        return issues;
    }
    
    /**
     * Create security issue object
     */
    private CodeRepository.SecurityIssue createSecurityIssue(
            SecurityPattern pattern, CodeRepository.CodeFile file, 
            int lineNumber, String line) {
        return CodeRepository.SecurityIssue.builder()
            .type(pattern.getType())
            .severity(pattern.getSeverity())
            .description(pattern.getDescription())
            .file(file.getPath())
            .line(lineNumber)
            .codeSnippet(line.trim().substring(0, Math.min(line.trim().length(), 200)))
            .remediation(pattern.getRemediation())
            .cweId(pattern.getCweId())
            .owaspCategory(getOwaspCategory(pattern.getType()))
            .isFalsePositive(false)
            .build();
    }
    
    /**
     * Check if match is a false positive
     */
    private boolean isFalsePositive(String line, SecurityPattern pattern) {
        // Skip comments
        if (line.trim().startsWith("//") || line.trim().startsWith("#") || 
            line.trim().startsWith("/*") || line.trim().startsWith("*")) {
            return true;
        }
        
        // Skip test files
        if (pattern.getType().equals("DEBUG_STATEMENT") && 
            (line.contains("test") || line.contains("Test"))) {
            return true;
        }
        
        // Skip example/documentation strings
        if (line.contains("example.com") || line.contains("your-") || 
            line.contains("replace-this")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Get OWASP category for vulnerability type
     */
    private String getOwaspCategory(String type) {
        switch (type) {
            case "SQL_INJECTION":
            case "SQL_INJECTION_CONCAT":
                return "A03:2021-Injection";
            case "XSS_HTML":
            case "XSS_EVAL":
                return "A03:2021-Injection";
            case "CMD_INJECTION":
                return "A03:2021-Injection";
            case "PATH_TRAVERSAL":
                return "A01:2021-Broken Access Control";
            case "HARDCODED_SECRET":
            case "AWS_SECRET":
            case "PRIVATE_KEY":
                return "A07:2021-Identification and Authentication Failures";
            case "MISSING_AUTH":
                return "A01:2021-Broken Access Control";
            case "WEAK_CRYPTO":
                return "A02:2021-Cryptographic Failures";
            case "INSECURE_RANDOM":
                return "A02:2021-Cryptographic Failures";
            default:
                return "A09:2021-Security Logging and Monitoring Failures";
        }
    }
    
    /**
     * Security pattern definition
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SecurityPattern {
        private String type;
        private String severity;
        private Pattern pattern;
        private String description;
        private String remediation;
        private String cweId;
    }
}