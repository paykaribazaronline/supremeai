package com.supremeai.ide.learning

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.project.Project
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Detects external AI tool usage by monitoring editor patterns
 * and clipboard content for AI-generated code.
 */
class ExternalAIDetector(private val project: Project) {
    
    private val detectionQueue = ConcurrentLinkedQueue<AIDetectionEvent>()
    private val knownPatterns = mutableListOf<AIPattern>()
    
    init {
        initializeKnownPatterns()
    }
    
    /**
     * Initialize known AI generation patterns
     */
    private fun initializeKnownPatterns() {
        // ChatGPT patterns
        knownPatterns.add(AIPattern(
            name = "ChatGPT Comment Style",
            pattern = Regex("//\\s*[Cc]hatGPT.*:"),
            provider = "ChatGPT",
            confidence = 0.8
        ))
        
        knownPatterns.add(AIPattern(
            name = "ChatGPT Code Block",
            pattern = Regex("```[\\w+]*\\n[\\s\\S]*?\\n```"),
            provider = "ChatGPT",
            confidence = 0.9
        ))
        
        // Claude patterns
        knownPatterns.add(AIPattern(
            name = "Claude Thinking Block",
            pattern = Regex("//\\s*<thinking>[\\s\\S]*?</thinking>"),
            provider = "Claude",
            confidence = 0.85
        ))
        
        // Generic AI patterns
        knownPatterns.add(AIPattern(
            name = "AI Generated Comment",
            pattern = Regex("//\\s*(Generated|Created|Written)\\s+(by|with)\\s+.*[Aa][Ii].*"),
            provider = "Unknown",
            confidence = 0.7
        ))
        
        knownPatterns.add(AIPattern(
            name = "AI Explanation Comment",
            pattern = Regex("//\\s*[Ee]xplanation:.*"),
            provider = "Unknown",
            confidence = 0.75
        ))
    }
    
    /**
     * Check if pasted content appears to be from an AI tool
     */
    fun checkPastedContent(content: String, sourceEditor: Editor?): AIDetectionResult {
        val detections = mutableListOf<AIDetection>()
        var maxConfidence = 0.0
        var detectedProvider = "Unknown"
        
        // Check against known patterns
        for (pattern in knownPatterns) {
            if (pattern.pattern.containsMatchIn(content)) {
                detections.add(AIDetection(
                    pattern = pattern.name,
                    confidence = pattern.confidence,
                    matchedText = pattern.pattern.find(content)?.value ?: ""
                ))
                
                if (pattern.confidence > maxConfidence) {
                    maxConfidence = pattern.confidence
                    detectedProvider = pattern.provider
                }
            }
        }
        
        // Check for AI-like code characteristics
        val aiCharacteristics = checkAICharacteristics(content)
        
        return AIDetectionResult(
            isAIGenerated = maxConfidence > 0.7 || aiCharacteristics.score > 0.8,
            confidence = maxOf(maxConfidence, aiCharacteristics.score),
            provider = detectedProvider,
            detections = detections,
            characteristics = aiCharacteristics,
            timestamp = System.currentTimeMillis()
        )
    }
    
    /**
     * Analyze code for AI-generation characteristics
     */
    private fun checkAICharacteristics(code: String): AICharacteristics {
        val lines = code.lines()
        val totalLines = lines.size
        
        if (totalLines == 0) {
            return AICharacteristics(score = 0.0)
        }
        
        var commentDensity = 0.0
        var averageLineLength = 0.0
        var hasDocstring = false
        var hasTypeHints = false
        var hasExplanatoryComments = false
        
        for (line in lines) {
            val trimmed = line.trim()
            
            // Check for comments
            if (trimmed.startsWith("//") || trimmed.startsWith("/*")) {
                commentDensity++
                
                // Check for explanatory comments
                if (trimmed.contains(Regex("(explanation|reason|because|since|therefore)", RegexOption.IGNORE_CASE))) {
                    hasExplanatoryComments = true
                }
            }
            
            // Check for docstrings (Python-style)
            if (trimmed.contains("\"\"\"") || trimmed.contains("'''") || 
                trimmed.matches(Regex("^\\s*/\\*\\*.*"))) {
                hasDocstring = true
            }
            
            // Check for type hints
            if (trimmed.contains(":") && trimmed.matches(Regex(".*:[ ]*[a-zA-Z_][a-zA-Z0-9_]*.*"))) {
                hasTypeHints = true
            }
            
            averageLineLength += line.length
        }
        
        commentDensity /= totalLines
        averageLineLength /= totalLines
        
        // AI-generated code often has:
        // - Higher comment density
        // - Consistent formatting
        // - Explanatory comments
        // - Type hints (in supported languages)
        // - Docstrings
        
        var score = 0.0
        
        if (commentDensity > 0.15) score += 0.2
        if (commentDensity > 0.25) score += 0.2
        if (hasExplanatoryComments) score += 0.3
        if (hasDocstring) score += 0.1
        if (hasTypeHints) score += 0.1
        
        // Check for consistent formatting (AI tends to be very consistent)
        val lineLengthVariance = calculateLineLengthVariance(lines)
        if (lineLengthVariance < 500) { // Low variance = consistent
            score += 0.1
        }
        
        return AICharacteristics(
            score = score.coerceIn(0.0, 1.0),
            commentDensity = commentDensity,
            averageLineLength = averageLineLength,
            hasDocstring = hasDocstring,
            hasTypeHints = hasTypeHints,
            hasExplanatoryComments = hasExplanatoryComments
        )
    }
    
    private fun calculateLineLengthVariance(lines: List<String>): Double {
        if (lines.size < 2) return 0.0
        
        val lengths = lines.map { it.length }
        val mean = lengths.average()
        val variance = lengths.map { (it - mean) * (it - mean) }.average()
        
        return variance
    }
    
    /**
     * Register document listener for automatic detection
     */
    fun registerDocumentListener(editor: Editor, callback: (AIDetectionResult) -> Unit) {
        editor.document.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                // Check if this looks like a paste operation
                if (event.newLength > 50 && event.oldLength == 0) { // Large insertion
                    val newText = event.newFragment.toString()
                    val result = checkPastedContent(newText, editor)
                    
                    if (result.isAIGenerated) {
                        callback(result)
                    }
                }
            }
        })
    }
    
    /**
     * Manually check code for AI generation
     */
    fun checkCode(code: String): AIDetectionResult {
        return checkPastedContent(code, null)
    }
}

/**
 * Represents an AI pattern to detect
 */
data class AIPattern(
    val name: String,
    val pattern: Regex,
    val provider: String,
    val confidence: Double
)

/**
 * Represents a single detection match
 */
data class AIDetection(
    val pattern: String,
    val confidence: Double,
    val matchedText: String
)

/**
 * Characteristics of AI-generated code
 */
data class AICharacteristics(
    val score: Double,
    val commentDensity: Double = 0.0,
    val averageLineLength: Double = 0.0,
    val hasDocstring: Boolean = false,
    val hasTypeHints: Boolean = false,
    val hasExplanatoryComments: Boolean = false
)

/**
 * Result of AI detection
 */
data class AIDetectionResult(
    val isAIGenerated: Boolean,
    val confidence: Double,
    val provider: String,
    val detections: List<AIDetection>,
    val characteristics: AICharacteristics,
    val timestamp: Long
)

/**
 * Event for detection queue
 */
data class AIDetectionEvent(
    val projectId: String,
    val file: String,
    val result: AIDetectionResult,
    val timestamp: Long
)