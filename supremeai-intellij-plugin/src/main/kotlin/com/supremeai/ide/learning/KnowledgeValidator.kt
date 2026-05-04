package com.supremeai.ide.learning

import com.intellij.openapi.diagnostic.logger

/**
 * Validates knowledge items before storage
 */
object KnowledgeValidator {
    
    private val logger = logger<KnowledgeValidator>()
    
    // Minimum quality thresholds
    private const val MIN_CONFIDENCE = 0.6
    private const val MIN_SECURITY_SCORE = 0.5
    private const val MIN_PERFORMANCE_SCORE = 0.4
    
    // Maximum sizes
    private const val MAX_PROMPT_LENGTH = 1000
    private const val MAX_RESPONSE_LENGTH = 10000
    
    /**
     * Validate knowledge item
     */
    fun validate(item: KnowledgeItem): ValidationResult {
        val reasons = mutableListOf<String>()
        
        // Check confidence
        if (item.quality.confidence < MIN_CONFIDENCE) {
            reasons.add("Confidence too low: ${item.quality.confidence} < $MIN_CONFIDENCE")
        }
        
        // Check security score
        if (item.quality.securityScore < MIN_SECURITY_SCORE) {
            reasons.add("Security score too low: ${item.quality.securityScore} < $MIN_SECURITY_SCORE")
        }
        
        // Check performance score
        if (item.quality.performanceScore < MIN_PERFORMANCE_SCORE) {
            reasons.add("Performance score too low: ${item.quality.performanceScore} < $MIN_PERFORMANCE_SCORE")
        }
        
        // Check prompt length
        if (item.prompt.length > MAX_PROMPT_LENGTH) {
            reasons.add("Prompt too long: ${item.prompt.length} > $MAX_PROMPT_LENGTH")
        }
        
        // Check response length
        if (item.response.length > MAX_RESPONSE_LENGTH) {
            reasons.add("Response too long: ${item.response.length} > $MAX_RESPONSE_LENGTH")
        }
        
        // Check for empty content
        if (item.prompt.isBlank()) {
            reasons.add("Prompt is empty")
        }
        
        if (item.response.isBlank()) {
            reasons.add("Response is empty")
        }
        
        // Check for profanity or inappropriate content
        if (containsProfanity(item.prompt) || containsProfanity(item.response)) {
            reasons.add("Contains inappropriate content")
        }
        
        // Check for spam patterns
        if (isSpam(item.prompt) || isSpam(item.response)) {
            reasons.add("Detected spam patterns")
        }
        
        // Check license compliance
        val licenseIssues = checkLicenseCompliance(item)
        reasons.addAll(licenseIssues)
        
        val isValid = reasons.isEmpty()
        
        logger.info("Knowledge validation ${if (isValid) "passed" else "failed"} for item ${item.id}: $reasons")
        
        return ValidationResult(
            isValid = isValid,
            reasons = reasons
        )
    }
    
    /**
     * Check for profanity
     */
    private fun containsProfanity(text: String): Boolean {
        val profanityList = listOf(
            // Add profanity words here
        )
        
        val lowercaseText = text.lowercase()
        return profanityList.any { it in lowercaseText }
    }
    
    /**
     * Check for spam patterns
     */
    private fun isSpam(text: String): Boolean {
        val spamPatterns = listOf(
            Regex("(.)\\1{5,}"), // Repeated characters
            Regex("https?://[^\\s\"]+"), // URLs (might be legitimate)
            Regex("[A-Z]{5,}"), // All caps
            Regex("\\$[a-zA-Z_][a-zA-Z0-9_]*"), // Variable names (might be legitimate)
        )
        
        // Count spam indicators
        val spamCount = spamPatterns.count { it.containsMatchIn(text) }
        
        // Consider it spam if multiple indicators are present
        return spamCount >= 3
    }
    
    /**
     * Check license compliance
     */
    private fun checkLicenseCompliance(item: KnowledgeItem): List<String> {
        val issues = mutableListOf<String>()
        
        // Check for GPL contamination in proprietary code
        if (item.tags.contains("proprietary")) {
            if (item.response.contains("GPL", RegexOption.IGNORE_CASE) ||
                item.response.contains("General Public License", RegexOption.IGNORE_CASE)) {
                issues.add("GPL contamination in proprietary code")
            }
        }
        
        // Check for license headers
        if (item.response.contains("Copyright", RegexOption.IGNORE_CASE) &&
            !item.response.contains("Apache License", RegexOption.IGNORE_CASE) &&
            !item.response.contains("MIT License", RegexOption.IGNORE_CASE)) {
            issues.add("Unknown license header detected")
        }
        
        return issues
    }
}

/**
 * Validation result
 */
data class ValidationResult(
    val isValid: Boolean,
    val reasons: List<String>
)