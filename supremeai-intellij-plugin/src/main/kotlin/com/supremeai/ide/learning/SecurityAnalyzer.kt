package com.supremeai.ide.learning

import com.intellij.openapi.diagnostic.logger

/**
 * Analyzes code for security vulnerabilities
 */
object SecurityAnalyzer {
    
    private val logger = logger<SecurityAnalyzer>()
    
    // Security patterns to check
    private val securityPatterns = listOf(
        SecurityPattern(
            name = "Hardcoded Secret",
            pattern = Regex("(password|secret|key|token|api_key)\\s*[=:]\\s*[\"'][^\"']+[\"']", RegexOption.IGNORE_CASE),
            severity = "HIGH",
            description = "Hardcoded credentials detected"
        ),
        SecurityPattern(
            name = "SQL Injection",
            pattern = Regex("(SELECT|INSERT|UPDATE|DELETE).*\\+.*\\$", RegexOption.IGNORE_CASE),
            severity = "CRITICAL",
            description = "Potential SQL injection vulnerability"
        ),
        SecurityPattern(
            name = "Command Injection",
            pattern = Regex("(Runtime\\.getRuntime\\(\\)\\.exec|ProcessBuilder)\\(.*\\+.*\\$"),
            severity = "CRITICAL",
            description = "Potential command injection vulnerability"
        ),
        SecurityPattern(
            name = "Path Traversal",
            pattern = Regex("(\\.\\.[/\\\\]|\\.\\.\\.)"),
            severity = "HIGH",
            description = "Potential path traversal vulnerability"
        ),
        SecurityPattern(
            name = "XSS Vulnerability",
            pattern = Regex("(innerHTML|outerHTML|document\\.write)\\s*=", RegexOption.IGNORE_CASE),
            severity = "HIGH",
            description = "Potential XSS vulnerability"
        ),
        SecurityPattern(
            name = "Insecure Random",
            pattern = Regex("Random\\(\\)|Math\\.random\\(\\)\\s*\\)"),
            severity = "MEDIUM",
            description = "Use SecureRandom for cryptographic operations"
        ),
        SecurityPattern(
            name = "Weak Cryptography",
            pattern = Regex("(MD5|SHA1|DES)\\b", RegexOption.IGNORE_CASE),
            severity = "HIGH",
            description = "Weak cryptographic algorithm"
        ),
        SecurityPattern(
            name = "Hardcoded IP",
            pattern = Regex("\\b(?:[0-9]{1,3}\\.){3}[0-9]{1,3}\\b"),
            severity = "LOW",
            description = "Hardcoded IP address"
        ),
        SecurityPattern(
            name = "Insecure HTTP",
            pattern = Regex("http://[^\\s\"]+", RegexOption.IGNORE_CASE),
            severity = "MEDIUM",
            description = "Use HTTPS instead of HTTP"
        )
    )
    
    /**
     * Analyze code for security vulnerabilities
     */
    fun analyze(code: String): SecurityAnalysis {
        val vulnerabilities = mutableListOf<Vulnerability>()
        val warnings = mutableListOf<String>()
        
        val lines = code.lines()
        
        for ((lineNumber, line) in lines.withIndex()) {
            for (pattern in securityPatterns) {
                if (pattern.pattern.containsMatchIn(line)) {
                    vulnerabilities.add(Vulnerability(
                        type = pattern.name,
                        severity = pattern.severity,
                        description = pattern.description,
                        line = lineNumber + 1
                    ))
                }
            }
            
            // Check for additional warnings
            if (line.contains("TODO", RegexOption.IGNORE_CASE) ||
                line.contains("FIXME", RegexOption.IGNORE_CASE) ||
                line.contains("HACK", RegexOption.IGNORE_CASE)) {
                warnings.add("Line ${lineNumber + 1}: Contains TODO/FIXME/HACK comment")
            }
            
            if (line.trim().startsWith("catch") && line.contains("Exception")) {
                warnings.add("Line ${lineNumber + 1}: Generic exception catch")
            }
        }
        
        // Calculate security score
        val score = calculateSecurityScore(vulnerabilities, warnings)
        
        return SecurityAnalysis(
            score = score,
            vulnerabilities = vulnerabilities,
            warnings = warnings
        )
    }
    
    /**
     * Calculate security score (0.0 to 1.0)
     */
    private fun calculateSecurityScore(
        vulnerabilities: List<Vulnerability>,
        warnings: List<String>
    ): Double {
        var score = 1.0
        
        // Deduct for vulnerabilities
        for (vuln in vulnerabilities) {
            when (vuln.severity) {
                "CRITICAL" -> score -= 0.3
                "HIGH" -> score -= 0.2
                "MEDIUM" -> score -= 0.1
                "LOW" -> score -= 0.05
            }
        }
        
        // Deduct for warnings
        score -= warnings.size * 0.02
        
        // Ensure score is in valid range
        return score.coerceIn(0.0, 1.0)
    }
}

/**
 * Security pattern definition
 */
data class SecurityPattern(
    val name: String,
    val pattern: Regex,
    val severity: String,
    val description: String
)