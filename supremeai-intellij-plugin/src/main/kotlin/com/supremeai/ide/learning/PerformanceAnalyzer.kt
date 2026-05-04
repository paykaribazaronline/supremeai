package com.supremeai.ide.learning

import com.intellij.openapi.diagnostic.logger

/**
 * Analyzes code for performance issues
 */
object PerformanceAnalyzer {
    
    private val logger = logger<PerformanceAnalyzer>()
    
    // Performance patterns to check
    private val performancePatterns = listOf(
        PerformancePattern(
            name = "Nested Loops",
            pattern = Regex("for\\s*\\(.*\\)\\s*{[^}]*for\\s*\\(.*\\)\\s*{"),
            severity = "MEDIUM",
            suggestion = "Consider optimizing nested loops or using more efficient algorithms"
        ),
        PerformancePattern(
            name = "String Concatenation in Loop",
            pattern = Regex("for\\s*\\(.*\\)\\s*{[^}]*\\+="),
            severity = "HIGH",
            suggestion = "Use StringBuilder for string concatenation in loops"
        ),
        PerformancePattern(
            name = "Boxing/Unboxing",
            pattern = Regex("Integer|Long|Double|Float|Boolean|Character"),
            severity = "LOW",
            suggestion = "Consider using primitive types to avoid boxing/unboxing overhead"
        ),
        PerformancePattern(
            name = "Synchronized Method",
            pattern = Regex("@Synchronized|synchronized\\s*\\("),
            severity = "MEDIUM",
            suggestion = "Consider using more granular synchronization or concurrent data structures"
        ),
        PerformancePattern(
            name = "Large Object Allocation",
            pattern = Regex("new\\s+(ByteArray|CharArray|IntArray|LongArray)\\s*\\(\\s*[0-9]{6,}"),
            severity = "HIGH",
            suggestion = "Large array allocation may cause memory pressure"
        ),
        PerformancePattern(
            name = "Reflection Usage",
            pattern = Regex("Class\\.forName|getMethod|getDeclaredMethod|invoke"),
            severity = "MEDIUM",
            suggestion = "Reflection is slow; consider alternatives"
        ),
        PerformancePattern(
            name = "Regex in Loop",
            pattern = Regex("for\\s*\\(.*\\)\\s*{[^}]*Regex\\s*\\("),
            severity = "HIGH",
            suggestion = "Pre-compile regex patterns outside of loops"
        ),
        PerformancePattern(
            name = "Database Query in Loop",
            pattern = Regex("for\\s*\\(.*\\)\\s*{[^}]*query|execute|select|insert|update|delete", RegexOption.IGNORE_CASE),
            severity = "CRITICAL",
            suggestion = "Avoid database queries in loops; use batch operations"
        )
    )
    
    /**
     * Analyze code for performance issues
     */
    fun analyze(code: String): PerformanceAnalysis {
        val issues = mutableListOf<PerformanceIssue>()
        val suggestions = mutableListOf<String>()
        
        val lines = code.lines()
        
        for ((lineNumber, line) in lines.withIndex()) {
            for (pattern in performancePatterns) {
                if (pattern.pattern.containsMatchIn(line)) {
                    issues.add(PerformanceIssue(
                        type = pattern.name,
                        severity = pattern.severity,
                        description = "Performance issue detected at line ${lineNumber + 1}",
                        suggestion = pattern.suggestion
                    ))
                }
            }
            
            // Check for additional suggestions
            if (line.contains("ArrayList()", RegexOption.IGNORE_CASE)) {
                suggestions.add("Consider specifying initial capacity for ArrayList")
            }
            
            if (line.contains("HashMap()", RegexOption.IGNORE_CASE)) {
                suggestions.add("Consider specifying initial capacity for HashMap")
            }
            
            if (line.contains("LinkedList", RegexOption.IGNORE_CASE)) {
                suggestions.add("Consider if ArrayList would be more efficient than LinkedList")
            }
        }
        
        // Calculate performance score
        val score = calculatePerformanceScore(issues, suggestions)
        
        return PerformanceAnalysis(
            score = score,
            issues = issues,
            suggestions = suggestions
        )
    }
    
    /**
     * Calculate performance score (0.0 to 1.0)
     */
    private fun calculatePerformanceScore(
        issues: List<PerformanceIssue>,
        suggestions: List<String>
    ): Double {
        var score = 1.0
        
        // Deduct for issues
        for (issue in issues) {
            when (issue.severity) {
                "CRITICAL" -> score -= 0.3
                "HIGH" -> score -= 0.2
                "MEDIUM" -> score -= 0.1
                "LOW" -> score -= 0.05
            }
        }
        
        // Deduct for suggestions
        score -= suggestions.size * 0.02
        
        // Ensure score is in valid range
        return score.coerceIn(0.0, 1.0)
    }
}

/**
 * Performance pattern definition
 */
data class PerformancePattern(
    val name: String,
    val pattern: Regex,
    val severity: String,
    val suggestion: String
)