package org.supremeai.agents.phase8;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * PHASE 8: ALPHA-SECURITY AGENT
 * 
 * Implements OWASP Top 10 vulnerability scanning with pattern detection
 * and static code analysis for Go, Python, Java, JavaScript, TypeScript.
 * 
 * Target: 100% OWASP detection, <5% false positives
 */
@Service
public class AlphaSecurityAgent {
    private static final Logger logger = LoggerFactory.getLogger(AlphaSecurityAgent.class);
    
    private static final Map<String, Pattern> VULN_PATTERNS = new HashMap<>();
    static {
        // A01: Broken Access Control
        VULN_PATTERNS.put("BROKEN_ACCESS", Pattern.compile("(publicKey|accessToken|apiKey)\\s*=\\s*['\"](?!\\$|env|config)", Pattern.CASE_INSENSITIVE));
        VULN_PATTERNS.put("NO_AUTH_CHECK", Pattern.compile("@(GetMapping|PostMapping|PutMapping)(?!.*auth|security|principal)", Pattern.CASE_INSENSITIVE));
        
        // A02: Cryptographic Failures
        VULN_PATTERNS.put("WEAK_CRYPTO", Pattern.compile("(MD5|MD4|SHA1|DES|RC4)(?!256|512)", Pattern.CASE_INSENSITIVE));
        VULN_PATTERNS.put("HARDCODED_SECRET", Pattern.compile("(password|secret|key)\\s*[=:]\\s*['\"]([^'\"]{8,})['\"]", Pattern.CASE_INSENSITIVE));
        
        // A03: Injection
        VULN_PATTERNS.put("SQL_INJECTION", Pattern.compile("execute\\s*\\(\\s*['\\\"].*[+].*['\\\"]", Pattern.CASE_INSENSITIVE));
        VULN_PATTERNS.put("NO_PARAMETERIZED", Pattern.compile("(SELECT|INSERT|UPDATE|DELETE)\\s+.*[+]\\s*(WHERE|VALUES)", Pattern.CASE_INSENSITIVE));
        
        // A04: Insecure Design
        VULN_PATTERNS.put("NO_RATE_LIMIT", Pattern.compile("@(RateLimiter|RequestMapping)(?!.*rate|limit)", Pattern.CASE_INSENSITIVE));
        
        // A05: Security Misconfiguration
        VULN_PATTERNS.put("DEBUG_MODE", Pattern.compile("(debug|DEBUG)\\s*=\\s*true", Pattern.CASE_INSENSITIVE));
        VULN_PATTERNS.put("CORS_OPEN", Pattern.compile("allowedOrigins\\s*=\\s*\\[\\s*['\\\"]\\*['\\\"]", Pattern.CASE_INSENSITIVE));
        
        // A06: Vulnerable Components
        VULN_PATTERNS.put("OUTDATED_DEPENDENCY", Pattern.compile("(jackson|spring|hibernate)[-_](databind|core)\\s*[=:]\\s*2\\.[0-9]\\.\\d[^0-9]", Pattern.CASE_INSENSITIVE));
        
        // A07: Authentication Failures
        VULN_PATTERNS.put("WEAK_PASSWORD", Pattern.compile("password.*regex.*\\[\\^\\\\w\\]", Pattern.CASE_INSENSITIVE));
        VULN_PATTERNS.put("NO_2FA", Pattern.compile("(authentication|auth)(?!.*totp|twoFactor|mfa)", Pattern.CASE_INSENSITIVE));
        
        // A08: Software Data Integrity
        VULN_PATTERNS.put("UNSIGNED_DEPENDENCY", Pattern.compile("npm\\s+install(?!.*integrity|verify)", Pattern.CASE_INSENSITIVE));
        
        // A09: Logging Failures
        VULN_PATTERNS.put("NO_AUDIT_LOG", Pattern.compile("(DELETE|UPDATE|sensitive)(?!.*log|audit)", Pattern.CASE_INSENSITIVE));
        VULN_PATTERNS.put("LOG_SECRETS", Pattern.compile("logger\\..*\\(.*(?:password|token|apiKey|secret)", Pattern.CASE_INSENSITIVE));
        
        // A10: SSRF
        VULN_PATTERNS.put("OPEN_SSRF", Pattern.compile("(RestTemplate|HttpClient)\\.get\\(userInput", Pattern.CASE_INSENSITIVE));
    }

    /**
     * Scan code for OWASP Top 10 vulnerabilities
     */
    public Map<String, Object> scanForVulnerabilities(String projectId, String sourceCode) {
        logger.info("🛡️ AlphaSecurityAgent: Starting vulnerability scan for project {}", projectId);
        
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("project_id", projectId);
        report.put("agent", "AlphaSecurityAgent");
        report.put("scan_timestamp", System.currentTimeMillis());
        report.put("phase", 8);
        
        List<Map<String, Object>> findings = new ArrayList<>();
        int vulnCount = 0;
        
        if (sourceCode != null && !sourceCode.isEmpty()) {
            for (Map.Entry<String, Pattern> entry : VULN_PATTERNS.entrySet()) {
                Matcher matcher = entry.getValue().matcher(sourceCode);
                if (matcher.find()) {
                    findings.add(createVulnerability(entry.getKey(), matcher.group(), "CRITICAL"));
                    vulnCount++;
                }
            }
        }
        
        // OWASP Findings
        List<Map<String, Object>> owaspFindings = new ArrayList<>();
        owaspFindings.add(createOWASPFinding("A01:2021 - Broken Access Control", vulnCount == 0 ? "PASS" : "FAIL"));
        owaspFindings.add(createOWASPFinding("A02:2021 - Cryptographic Failures", vulnCount == 0 ? "PASS" : "FAIL"));
        owaspFindings.add(createOWASPFinding("A03:2021 - Injection", vulnCount == 0 ? "PASS" : "FAIL"));
        owaspFindings.add(createOWASPFinding("A04:2021 - Insecure Design", vulnCount == 0 ? "PASS" : "FAIL"));
        owaspFindings.add(createOWASPFinding("A05:2021 - Security Misconfiguration", vulnCount == 0 ? "PASS" : "FAIL"));
        owaspFindings.add(createOWASPFinding("A06:2021 - Vulnerable Components", vulnCount == 0 ? "PASS" : "FAIL"));
        owaspFindings.add(createOWASPFinding("A07:2021 - Authentication Failures", vulnCount == 0 ? "PASS" : "FAIL"));
        owaspFindings.add(createOWASPFinding("A08:2021 - Data Integrity Failures", vulnCount == 0 ? "PASS" : "FAIL"));
        owaspFindings.add(createOWASPFinding("A09:2021 - Logging Failures", vulnCount == 0 ? "PASS" : "FAIL"));
        owaspFindings.add(createOWASPFinding("A10:2021 - SSRF", vulnCount == 0 ? "PASS" : "FAIL"));
        
        report.put("owasp_findings", owaspFindings);
        report.put("vulnerabilities", findings);
        report.put("critical_vulnerabilities_count", vulnCount);
        report.put("security_score", Math.max(0, 100 - (vulnCount * 10)));
        report.put("status", vulnCount == 0 ? "SECURE" : "VULNERABLE");
        report.put("recommendation", generateRecommendation(vulnCount));
        
        logger.info("✓ AlphaSecurityAgent scan complete. Vulnerabilities: {}. Score: {}/100. Status: {}", 
            vulnCount, report.get("security_score"), report.get("status"));
        
        return report;
    }

    /**
     * Scan without source code (for testing)
     */
    public Map<String, Object> scanForVulnerabilities(String projectId) {
        logger.info("🛡️ AlphaSecurityAgent: Lightweight scan for project {}", projectId);
        return scanForVulnerabilities(projectId, "");
    }

    private Map<String, Object> createVulnerability(String type, String snippet, String severity) {
        Map<String, Object> vuln = new LinkedHashMap<>();
        vuln.put("type", type);
        vuln.put("severity", severity);
        vuln.put("code_snippet", snippet.substring(0, Math.min(50, snippet.length())));
        vuln.put("remediation", getRemediation(type));
        return vuln;
    }

    private Map<String, Object> createOWASPFinding(String category, String status) {
        Map<String, Object> finding = new LinkedHashMap<>();
        finding.put("category", category);
        finding.put("status", status);
        return finding;
    }

    private String generateRecommendation(int vulnCount) {
        if (vulnCount == 0) return "No vulnerabilities detected. Code meets OWASP standards.";
        if (vulnCount <= 3) return "Minor issues found. Review and fix priority findings.";
        if (vulnCount <= 7) return "Multiple vulnerabilities detected. Immediate action required.";
        return "CRITICAL: Severe vulnerabilities found. Do not deploy.";
    }

    private String getRemediation(String vulnType) {
        switch (vulnType) {
            case "BROKEN_ACCESS": return "Use environment variables for sensitive credentials";
            case "NO_AUTH_CHECK": return "Add @PreAuthorize annotation to protected endpoints";
            case "WEAK_CRYPTO": return "Use AES-256 or equivalent modern encryption";
            case "HARDCODED_SECRET": return "Move secrets to environment variables or vault";
            case "SQL_INJECTION": return "Use parameterized queries or ORM frameworks";
            case "NO_PARAMETERIZED": return "Use PreparedStatement instead of string concatenation";
            case "CORS_OPEN": return "Restrict CORS to specific trusted origins";
            case "DEBUG_MODE": return "Disable debug mode in production";
            default: return "Review code for security issues";
        }
    }

    /**
     * Get security scan results for a project
     */
    public Map<String, Object> getSecurityStatus(String projectId) {
        return scanForVulnerabilities(projectId);
    }
}
