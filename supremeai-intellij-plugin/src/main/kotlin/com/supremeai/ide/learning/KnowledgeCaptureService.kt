spackage com.supremeai.ide.learning

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.supremeai.ide.SupremeAISettings
import java.time.Instant
import java.util.UUID

/**
 * Captures and stores knowledge from external AI interactions
 */
@Service(Service.Level.PROJECT)
class KnowledgeCaptureService(private val project: Project) {
    
    private val localStore = LocalKnowledgeStore.getInstance()
    private val detector = ExternalAIDetector(project)
    
    /**
     * Capture knowledge from AI-generated code
     */
    fun captureAIGeneratedCode(
        code: String,
        context: CodeContext,
        detectionResult: AIDetectionResult,
        userFeedback: UserFeedback? = null
    ): KnowledgeItem {
        
        val knowledgeItem = KnowledgeItem(
            id = UUID.randomUUID().toString(),
            type = KnowledgeType.CODE_SUGGESTION,
            provider = detectionResult.provider,
            prompt = context.userPrompt ?: "Code generation request",
            response = code,
            context = KnowledgeContext(
                file = context.filePath,
                language = context.language,
                project = context.project,
                lineStart = context.lineStart,
                lineEnd = context.lineEnd
            ),
            quality = KnowledgeQuality(
                confidence = detectionResult.confidence,
                complexity = calculateComplexity(code),
                securityScore = SecurityAnalyzer.analyze(code).score,
                performanceScore = PerformanceAnalyzer.analyze(code).score
            ),
            feedback = userFeedback?.let {
                KnowledgeFeedback(
                    accepted = it.accepted,
                    modified = it.modified,
                    rating = it.rating,
                    comments = it.comments
                )
            },
            tags = generateTags(code, context),
            metadata = KnowledgeMetadata(
                capturedAt = Instant.now(),
                capturedBy = System.getProperty("user.name") ?: "unknown",
                source = "intellij-plugin",
                version = "1.2.0"
            ),
            synced = false
        )
        
        // Validate before storing
        val validationResult = KnowledgeValidator.validate(knowledgeItem)
        
        if (validationResult.isValid) {
            localStore.save(knowledgeItem)
            
            // Queue for sync
            if (SupremeAISettings.getInstance().shareMode != "disabled") {
                KnowledgeSyncQueue.add(knowledgeItem)
            }
            
            // Notify listeners
            KnowledgeCaptureEventBus.publish(
                KnowledgeCapturedEvent(knowledgeItem)
            )
        } else {
            // Store rejected items for review
            localStore.saveRejected(knowledgeItem, validationResult.reasons)
        }
        
        return knowledgeItem
    }
    
    /**
     * Capture knowledge from AI explanation
     */
    fun captureAIExplanation(
        explanation: String,
        context: CodeContext,
        relatedCode: String? = null
    ): KnowledgeItem {
        
        val knowledgeItem = KnowledgeItem(
            id = UUID.randomUUID().toString(),
            type = KnowledgeType.EXPLANATION,
            provider = "Unknown", // Will be detected from explanation style
            prompt = context.userPrompt ?: "Request for explanation",
            response = explanation,
            context = KnowledgeContext(
                file = context.filePath,
                language = context.language,
                project = context.project,
                lineStart = context.lineStart,
                lineEnd = context.lineEnd
            ),
            relatedCode = relatedCode,
            quality = KnowledgeQuality(
                confidence = 0.8, // Default for explanations
                complexity = calculateComplexity(explanation),
                securityScore = 1.0, // Explanations don't have security issues
                performanceScore = 1.0
            ),
            feedback = null,
            tags = listOf("explanation", context.language),
            metadata = KnowledgeMetadata(
                capturedAt = Instant.now(),
                capturedBy = System.getProperty("user.name") ?: "unknown",
                source = "intellij-plugin",
                version = "1.2.0"
            ),
            synced = false
        )
        
        localStore.save(knowledgeItem)
        
        return knowledgeItem
    }
    
    /**
     * Capture knowledge from AI bug fix
     */
    fun captureBugFix(
        originalCode: String,
        fixedCode: String,
        bugDescription: String,
        context: CodeContext
    ): KnowledgeItem {
        
        val knowledgeItem = KnowledgeItem(
            id = UUID.randomUUID().toString(),
            type = KnowledgeType.BUG_FIX,
            provider = "Unknown",
            prompt = bugDescription,
            response = fixedCode,
            context = KnowledgeContext(
                file = context.filePath,
                language = context.language,
                project = context.project,
                lineStart = context.lineStart,
                lineEnd = context.lineEnd
            ),
            relatedCode = originalCode,
            quality = KnowledgeQuality(
                confidence = 0.9, // Bug fixes are usually reliable
                complexity = calculateComplexity(fixedCode),
                securityScore = SecurityAnalyzer.analyze(fixedCode).score,
                performanceScore = PerformanceAnalyzer.analyze(fixedCode).score
            ),
            feedback = null,
            tags = listOf("bug-fix", "correction", context.language),
            metadata = KnowledgeMetadata(
                capturedAt = Instant.now(),
                capturedBy = System.getProperty("user.name") ?: "unknown",
                source = "intellij-plugin",
                version = "1.2.0"
            ),
            synced = false
        )
        
        localStore.save(knowledgeItem)
        
        return knowledgeItem
    }
    
    /**
     * Calculate code complexity
     */
    private fun calculateComplexity(code: String): Complexity {
        val lines = code.lines()
        val cyclomaticComplexity = calculateCyclomaticComplexity(code)
        val maintainabilityIndex = calculateMaintainabilityIndex(code)
        
        return when {
            cyclomaticComplexity > 10 || maintainabilityIndex < 65 -> Complexity.HIGH
            cyclomaticComplexity > 5 || maintainabilityIndex < 85 -> Complexity.MEDIUM
            else -> Complexity.LOW
        }
    }
    
    /**
     * Calculate cyclomatic complexity
     */
    private fun calculateCyclomaticComplexity(code: String): Int {
        val decisionPoints = listOf(
            "if", "else if", "for", "while", "case", "catch", "&&", "||"
        )
        
        return decisionPoints.sumOf { pattern ->
            Regex("\\b$pattern\\b").findAll(code).count()
        } + 1 // Base complexity
    }
    
    /**
     * Calculate maintainability index
     */
    private fun calculateMaintainabilityIndex(code: String): Int {
        val lines = code.lines()
        val commentLines = lines.count { it.trim().startsWith("//") }
        val codeLines = lines.size - commentLines
        
        // Simplified maintainability index
        val commentRatio = if (codeLines > 0) commentLines.toDouble() / codeLines else 0.0
        val avgLineLength = lines.map { it.length }.average()
        
        var score = 100
        
        // Penalize for long lines
        if (avgLineLength > 100) score -= 10
        if (avgLineLength > 150) score -= 20
        
        // Reward for comments
        if (commentRatio > 0.1) score += 10
        if (commentRatio > 0.2) score += 10
        
        // Penalize for very short or very long methods
        if (codeLines < 3) score -= 20
        if (codeLines > 50) score -= 20
        if (codeLines > 100) score -= 30
        
        return score.coerceIn(0, 100)
    }
    
    /**
     * Generate tags for knowledge item
     */
    private fun generateTags(code: String, context: CodeContext): List<String> {
        val tags = mutableListOf<String>()
        
        tags.add(context.language)
        
        // Detect framework
        if (code.contains("android", RegexOption.IGNORE_CASE)) {
            tags.add("android")
        }
        if (code.contains("compose", RegexOption.IGNORE_CASE)) {
            tags.add("jetpack-compose")
        }
        if (code.contains("viewmodel", RegexOption.IGNORE_CASE)) {
            tags.add("viewmodel")
        }
        
        // Detect common patterns
        if (code.contains("interface", RegexOption.IGNORE_CASE)) {
            tags.add("interface")
        }
        if (code.contains("class", RegexOption.IGNORE_CASE)) {
            tags.add("class")
        }
        if (code.contains("fun ", RegexOption.IGNORE_CASE)) {
            tags.add("function")
        }
        
        // Detect complexity
        val complexity = calculateComplexity(code)
        tags.add(complexity.name.lowercase())
        
        return tags.distinct()
    }
}

/**
 * Context for code generation
 */
data class CodeContext(
    val filePath: String,
    val language: String,
    val project: String,
    val lineStart: Int,
    val lineEnd: Int,
    val userPrompt: String? = null
)

/**
 * User feedback on knowledge
 */
data class UserFeedback(
    val accepted: Boolean,
    val modified: Boolean = false,
    val rating: Int? = null,
    val comments: String? = null
)

/**
 * Knowledge types
 */
enum class KnowledgeType {
    CODE_SUGGESTION,
    BUG_FIX,
    EXPLANATION,
    REFACTORING,
    OPTIMIZATION
}

/**
 * Complexity levels
 */
enum class Complexity {
    LOW, MEDIUM, HIGH
}