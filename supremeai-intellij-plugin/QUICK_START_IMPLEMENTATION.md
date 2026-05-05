# Quick Start Implementation Guide
## External AI Learning Integration - Phase 1

This guide provides the immediate implementation steps to enable the SupremeAI IntelliJ Plugin to learn from external AI providers.

## Immediate Actions (Can Start Now)

### 1. Create External AI Detector Class

**File:** `src/main/kotlin/com/supremeai/ide/learning/ExternalAIDetector.kt`

```kotlin
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
    fun checkPastedContent(content: String, sourceEditor: Editor): AIDetectionResult {
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
```

### 2. Create Knowledge Capture Service

**File:** `src/main/kotlin/com/supremeai/ide/learning/KnowledgeCaptureService.kt`

```kotlin
package com.supremeai.ide.learning

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
                project = project.name,
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
                project = project.name,
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
                project = project.name,
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
            "if", "else if", "for", "while", "case", "catch", "&&", "\\|\\|"
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
```

### 3. Create Local Knowledge Store

**File:** `src/main/kotlin/com/supremeai/ide/learning/LocalKnowledgeStore.kt`

```kotlin
package com.supremeai.ide.learning

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import java.io.File
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import kotlinx.serialization.*
import kotlinx.serialization.json.*

/**
 * Local storage for knowledge items
 */
@Service(Service.Level.PROJECT)
class LocalKnowledgeStore private constructor() {
    
    private val logger = logger<LocalKnowledgeStore>()
    private val knowledgeMap = ConcurrentHashMap<String, KnowledgeItem>()
    private val rejectedMap = ConcurrentHashMap<String, RejectedKnowledge>()
    private val storageDir = getStorageDirectory()
    
    companion object {
        private var instance: LocalKnowledgeStore? = null
        
        fun getInstance(): LocalKnowledgeStore {
            return instance ?: synchronized(this) {
                instance ?: LocalKnowledgeStore().also { instance = it }
            }
        }
    }
    
    init {
        loadFromDisk()
    }
    
    /**
     * Save knowledge item
     */
    fun save(item: KnowledgeItem) {
        knowledgeMap[item.id] = item
        persistToDisk()
        
        logger.info("Saved knowledge item: ${item.id} from ${item.provider}")
    }
    
    /**
     * Save rejected knowledge item
     */
    fun saveRejected(item: KnowledgeItem, reasons: List<String>) {
        val rejected = RejectedKnowledge(
            item = item,
            rejectedAt = Instant.now(),
            reasons = reasons
        )
        rejectedMap[item.id] = rejected
        persistRejectedToDisk()
        
        logger.warn("Rejected knowledge item: ${item.id} - $reasons")
    }
    
    /**
     * Get knowledge item by ID
     */
    fun get(id: String): KnowledgeItem? {
        return knowledgeMap[id]
    }
    
    /**
     * Get all knowledge items
     */
    fun getAll(): List<KnowledgeItem> {
        return knowledgeMap.values.toList()
    }
    
    /**
     * Get knowledge items by tag
     */
    fun getByTag(tag: String): List<KnowledgeItem> {
        return knowledgeMap.values.filter { it.tags.contains(tag) }
    }
    
    /**
     * Get knowledge items by provider
     */
    fun getByProvider(provider: String): List<KnowledgeItem> {
        return knowledgeMap.values.filter { it.provider == provider }
    }
    
    /**
     * Search knowledge items
     */
    fun search(query: String): List<KnowledgeItem> {
        val lowercaseQuery = query.lowercase()
        
        return knowledgeMap.values.filter { item ->
            item.prompt.lowercase().contains(lowercaseQuery) ||
            item.response.lowercase().contains(lowercaseQuery) ||
            item.tags.any { it.lowercase().contains(lowercaseQuery) } ||
            item.context.file.lowercase().contains(lowercaseQuery)
        }
    }
    
    /**
     * Mark item as synced
     */
    fun markAsSynced(id: String) {
        knowledgeMap[id]?.let { item ->
            val updated = item.copy(synced = true)
            knowledgeMap[id] = updated
            persistToDisk()
        }
    }
    
    /**
     * Get unsynced items
     */
    fun getUnsynced(): List<KnowledgeItem> {
        return knowledgeMap.values.filter { !it.synced }
    }
    
    /**
     * Delete knowledge item
     */
    fun delete(id: String) {
        knowledgeMap.remove(id)
        persistToDisk()
        
        logger.info("Deleted knowledge item: $id")
    }
    
    /**
     * Clear all knowledge
     */
    fun clear() {
        knowledgeMap.clear()
        persistToDisk()
        
        logger.info("Cleared all knowledge items")
    }
    
    /**
     * Get statistics
     */
    fun getStatistics(): KnowledgeStatistics {
        val items = knowledgeMap.values.toList()
        
        return KnowledgeStatistics(
            totalItems = items.size,
            totalRejected = rejectedMap.size,
            byProvider = items.groupBy { it.provider }
                .mapValues { it.value.size },
            byType = items.groupBy { it.type }
                .mapValues { it.value.size },
            byTag = items.flatMap { it.tags }
                .groupingBy { it }
                .eachCount(),
            averageConfidence = items.map { it.quality.confidence }
                .average().takeIf { items.isNotEmpty() } ?: 0.0,
            syncedItems = items.count { it.synced },
            unsyncedItems = items.count { !it.synced }
        )
    }
    
    /**
     * Get storage directory
     */
    private fun getStorageDirectory(): File {
        val projectDir = File(System.getProperty("user.home"), ".supremeai")
        if (!projectDir.exists()) {
            projectDir.mkdirs()
        }
        
        val knowledgeDir = File(projectDir, "knowledge")
        if (!knowledgeDir.exists()) {
            knowledgeDir.mkdirs()
        }
        
        return knowledgeDir
    }
    
    /**
     * Persist knowledge to disk
     */
    private fun persistToDisk() {
        try {
            val file = File(storageDir, "knowledge.json")
            val json = Json { 
                prettyPrint = true
                ignoreUnknownKeys = true
            }
            
            val items = knowledgeMap.values.toList()
            val jsonString = json.encodeToString(items)
            
            file.writeText(jsonString)
        } catch (e: Exception) {
            logger.error("Failed to persist knowledge to disk", e)
        }
    }
    
    /**
     * Persist rejected knowledge to disk
     */
    private fun persistRejectedToDisk() {
        try {
            val file = File(storageDir, "rejected.json")
            val json = Json { 
                prettyPrint = true
                ignoreUnknownKeys = true
            }
            
            val rejected = rejectedMap.values.toList()
            val jsonString = json.encodeToString(rejected)
            
            file.writeText(jsonString)
        } catch (e: Exception) {
            logger.error("Failed to persist rejected knowledge to disk", e)
        }
    }
    
    /**
     * Load knowledge from disk
     */
    private fun loadFromDisk() {
        try {
            val file = File(storageDir, "knowledge.json")
            if (!file.exists()) {
                return
            }
            
            val json = Json { 
                ignoreUnknownKeys = true
            }
            
            val jsonString = file.readText()
            val items: List<KnowledgeItem> = json.decodeFromString(jsonString)
            
            items.forEach { item ->
                knowledgeMap[item.id] = item
            }
            
            logger.info("Loaded ${items.size} knowledge items from disk")
        } catch (e: Exception) {
            logger.error("Failed to load knowledge from disk", e)
        }
        
        try {
            val file = File(storageDir, "rejected.json")
            if (!file.exists()) {
                return
            }
            
            val json = Json { 
                ignoreUnknownKeys = true
            }
            
            val jsonString = file.readText()
            val rejected: List<RejectedKnowledge> = json.decodeFromString(jsonString)
            
            rejected.forEach { item ->
                rejectedMap[item.item.id] = item
            }
            
            logger.info("Loaded ${rejected.size} rejected items from disk")
        } catch (e: Exception) {
            logger.error("Failed to load rejected knowledge from disk", e)
        }
    }
}

/**
 * Rejected knowledge item
 */
@Serializable
data class RejectedKnowledge(
    val item: KnowledgeItem,
    val rejectedAt: Instant,
    val reasons: List<String>
)

/**
 * Knowledge statistics
 */
@Serializable
data class KnowledgeStatistics(
    val totalItems: Int,
    val totalRejected: Int,
    val byProvider: Map<String, Int>,
    val byType: Map<KnowledgeType, Int>,
    val byTag: Map<String, Int>,
    val averageConfidence: Double,
    val syncedItems: Int,
    val unsyncedItems: Int
)
```

### 4. Create Knowledge Data Models

**File:** `src/main/kotlin/com/supremeai/ide/learning/KnowledgeModels.kt`

```kotlin
package com.supremeai.ide.learning

import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * Knowledge item
 */
@Serializable
data class KnowledgeItem(
    val id: String,
    val type: KnowledgeType,
    val provider: String,
    val prompt: String,
    val response: String,
    val context: KnowledgeContext,
    val relatedCode: String? = null,
    val quality: KnowledgeQuality,
    val feedback: KnowledgeFeedback? = null,
    val tags: List<String>,
    val metadata: KnowledgeMetadata,
    val synced: Boolean = false
)

/**
 * Knowledge context
 */
@Serializable
data class KnowledgeContext(
    val file: String,
    val language: String,
    val project: String,
    val lineStart: Int,
    val lineEnd: Int
)

/**
 * Knowledge quality
 */
@Serializable
data class KnowledgeQuality(
    val confidence: Double,
    val complexity: Complexity,
    val securityScore: Double,
    val performanceScore: Double
)

/**
 * Knowledge feedback
 */
@Serializable
data class KnowledgeFeedback(
    val accepted: Boolean,
    val modified: Boolean = false,
    val rating: Int? = null,
    val comments: String? = null
)

/**
 * Knowledge metadata
 */
@Serializable
data class KnowledgeMetadata(
    val capturedAt: Instant,
    val capturedBy: String,
    val source: String,
    val version: String
)

/**
 * Security analysis result
 */
@Serializable
data class SecurityAnalysis(
    val score: Double,
    val vulnerabilities: List<Vulnerability>,
    val warnings: List<String>
)

/**
 * Vulnerability
 */
@Serializable
data class Vulnerability(
    val type: String,
    val severity: String,
    val description: String,
    val line: Int? = null
)

/**
 * Performance analysis result
 */
@Serializable
data class PerformanceAnalysis(
    val score: Double,
    val issues: List<PerformanceIssue>,
    val suggestions: List<String>
)

/**
 * Performance issue
 */
@Serializable
data class PerformanceIssue(
    val type: String,
    val severity: String,
    val description: String,
    val suggestion: String
)
```

### 5. Create Security and Performance Analyzers

**File:** `src/main/kotlin/com/supremeai/ide/learning/SecurityAnalyzer.kt`

```kotlin
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
            pattern = Regex("(password|secret|key|token|api_key)\s*[=:]\s*[\"'][^\"']+[\"']", RegexOption.IGNORE_CASE),
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
```

**File:** `src/main/kotlin/com/supremeai/ide/learning/PerformanceAnalyzer.kt`

```kotlin
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
```

### 6. Create Knowledge Validator

**File:** `src/main/kotlin/com/supremeai/ide/learning/KnowledgeValidator.kt`

```kotlin
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
            Regex("https?://[^\\s]+"), // URLs (might be legitimate)
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
```

### 7. Create Knowledge Sync Queue

**File:** `src/main/kotlin/com/supremeai/ide/learning/KnowledgeSyncQueue.kt`

```kotlin
package com.supremeai.ide.learning

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Queue for syncing knowledge items to backend
 */
object KnowledgeSyncQueue {
    
    private val logger = logger<KnowledgeSyncQueue>()
    private val queue = ConcurrentLinkedQueue<KnowledgeItem>()
    private val executor = Executors.newSingleThreadScheduledExecutor()
    private var isRunning = false
    
    private const val SYNC_INTERVAL = 30L // seconds
    private const val BATCH_SIZE = 10
    
    init {
        startSyncScheduler()
    }
    
    /**
     * Add item to sync queue
     */
    fun add(item: KnowledgeItem) {
        queue.offer(item)
        logger.debug("Added item ${item.id} to sync queue")
    }
    
    /**
     * Add multiple items to sync queue
     */
    fun addAll(items: List<KnowledgeItem>) {
        items.forEach { add(it) }
    }
    
    /**
     * Start sync scheduler
     */
    private fun startSyncScheduler() {
        if (isRunning) return
        
        isRunning = true
        
        executor.scheduleAtFixedRate({
            try {
                syncBatch()
            } catch (e: Exception) {
                logger.error("Error during sync", e)
            }
        }, SYNC_INTERVAL, SYNC_INTERVAL, TimeUnit.SECONDS)
        
        logger.info("Sync scheduler started")
    }
    
    /**
     * Sync a batch of items
     */
    private fun syncBatch() {
        if (queue.isEmpty()) {
            return
        }
        
        val items = mutableListOf<KnowledgeItem>()
        for (i in 0 until BATCH_SIZE) {
            val item = queue.poll() ?: break
            items.add(item)
        }
        
        if (items.isNotEmpty()) {
            logger.info("Syncing batch of ${items.size} items")
            
            // Sync in background thread
            ApplicationManager.getApplication().executeOnPooledThread {
                try {
                    val success = KnowledgeSyncService.sync(items)
                    
                    if (success) {
                        items.forEach { item ->
                            LocalKnowledgeStore.getInstance().markAsSynced(item.id)
                        }
                        logger.info("Successfully synced ${items.size} items")
                    } else {
                        // Re-queue failed items
                        items.forEach { queue.offer(it) }
                        logger.warn("Failed to sync ${items.size} items, re-queued")
                    }
                } catch (e: Exception) {
                    logger.error("Error syncing items", e)
                    // Re-queue on error
                    items.forEach { queue.offer(it) }
                }
            }
        }
    }
    
    /**
     * Force sync all queued items
     */
    fun forceSync() {
        logger.info("Forcing sync of all queued items")
        syncBatch()
    }
    
    /**
     * Get queue size
     */
    fun size(): Int {
        return queue.size
    }
    
    /**
     * Check if queue is empty
     */
    fun isEmpty(): Boolean {
        return queue.isEmpty()
    }
    
    /**
     * Clear queue
     */
    fun clear() {
        queue.clear()
        logger.info("Cleared sync queue")
    }
    
    /**
     * Shutdown sync scheduler
     */
    fun shutdown() {
        isRunning = false
        executor.shutdown()
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow()
            }
        } catch (e: InterruptedException) {
            executor.shutdownNow()
        }
        logger.info("Sync scheduler shutdown")
    }
}
```

### 8. Create Knowledge Sync Service

**File:** `src/main/kotlin/com/supremeai/ide/learning/KnowledgeSyncService.kt`

```kotlin
package com.supremeai.ide.learning

import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.supremeai.ide.SupremeAISettings
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

/**
 * Service for syncing knowledge with backend
 */
object KnowledgeSyncService {
    
    private val logger = logger<KnowledgeSyncService>()
    private val json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    private const val SYNC_API_URL = "https://supremeai-lhlwyikwlq-uc.a.run.app/api/knowledge/sync"
    private const val SYNC_BATCH_SIZE = 10
    
    /**
     * Sync knowledge items to backend
     */
    fun sync(items: List<KnowledgeItem>): Boolean {
        if (items.isEmpty()) {
            return true
        }
        
        val settings = SupremeAISettings.getInstance()
        val apiKey = settings.apiKey.takeIf { it.isNotBlank() }
            ?: "dev-admin-token-local"
        
        return try {
            // Split into batches
            val batches = items.chunked(SYNC_BATCH_SIZE)
            var allSuccess = true
            
            for (batch in batches) {
                val success = syncBatch(batch, apiKey)
                if (!success) {
                    allSuccess = false
                    break
                }
            }
            
            allSuccess
        } catch (e: Exception) {
            logger.error("Error syncing knowledge", e)
            false
        }
    }
    
    /**
     * Sync a single batch
     */
    private fun syncBatch(items: List<KnowledgeItem>, apiKey: String): Boolean {
        return try {
            val url = URL(SYNC_API_URL)
            val connection = url.openConnection() as HttpURLConnection
            
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
            connection.setRequestProperty("X-Client-Version", "1.2.0")
            connection.setRequestProperty("X-Client-Source", "intellij-plugin")
            connection.doOutput = true
            connection.connectTimeout = 10000
            connection.readTimeout = 30000
            
            // Create sync request
            val request = SyncRequest(
                items = items,
                source = "intellij-plugin",
                version = "1.2.0"
            )
            
            val jsonString = json.encodeToString(request)
            connection.outputStream.use { it.write(jsonString.toByteArray()) }
            
            val responseCode = connection.responseCode
            
            if (responseCode == HttpURLConnection.HTTP_OK ||
                responseCode == HttpURLConnection.HTTP_CREATED) {
                logger.info("Successfully synced ${items.size} items")
                true
            } else {
                logger.error("Sync failed with response code: $responseCode")
                false
            }
        } catch (e: Exception) {
            logger.error("Error syncing batch", e)
            false
        }
    }
    
    /**
     * Fetch knowledge from backend
     */
    fun fetch(since: Long? = null): List<KnowledgeItem> {
        return try {
            val settings = SupremeAISettings.getInstance()
            val apiKey = settings.apiKey.takeIf { it.isNotBlank() }
                ?: "dev-admin-token-local"
            
            val url = URL("$SYNC_API_URL?since=${since ?: 0}")
            val connection = url.openConnection() as HttpURLConnection
            
            connection.requestMethod = "GET"
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
            connection.setRequestProperty("X-Client-Version", "1.2.0")
            connection.connectTimeout = 10000
            connection.readTimeout = 30000
            
            val responseCode = connection.responseCode
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val syncResponse = json.decodeFromString<SyncResponse>(response)
                syncResponse.items
            } else {
                logger.error("Fetch failed with response code: $responseCode")
                emptyList()
            }
        } catch (e: Exception) {
            logger.error("Error fetching knowledge", e)
            emptyList()
        }
    }
    
    /**
     * Sync request
     */
    @Serializable
    data class SyncRequest(
        val items: List<KnowledgeItem>,
        val source: String,
        val version: String
    )
    
    /**
     * Sync response
     */
    @Serializable
    data class SyncResponse(
        val items: List<KnowledgeItem>,
        val total: Int,
        val syncedAt: Long
    )
}
```

### 9. Create Event Bus for Knowledge Events

**File:** `src/main/kotlin/com/supremeai/ide/learning/KnowledgeEventBus.kt`

```kotlin
package com.supremeai.ide.learning

import com.intellij.util.messages.Topic
import com.intellij.openapi.project.Project

/**
 * Event bus for knowledge-related events
 */
object KnowledgeCaptureEventBus {
    
    val TOPIC = Topic.create("KnowledgeCaptureEvent", KnowledgeCaptureListener::class.java)
    
    fun publish(event: KnowledgeEvent) {
        // Implementation depends on IntelliJ's message bus
        // This is a simplified version
    }
}

/**
 * Knowledge event types
 */
sealed class KnowledgeEvent {
    data class KnowledgeCapturedEvent(val item: KnowledgeItem) : KnowledgeEvent()
    data class KnowledgeSyncedEvent(val items: List<KnowledgeItem>) : KnowledgeEvent()
    data class KnowledgeRejectedEvent(val item: KnowledgeItem, val reasons: List<String>) : KnowledgeEvent()
}

/**
 * Knowledge capture listener
 */
interface KnowledgeCaptureListener {
    fun onKnowledgeCaptured(event: KnowledgeEvent.KnowledgeCapturedEvent)
    fun onKnowledgeSynced(event: KnowledgeEvent.KnowledgeSyncedEvent)
    fun onKnowledgeRejected(event: KnowledgeEvent.KnowledgeRejectedEvent)
}
```

### 10. Create UI Components for Knowledge Management

**File:** `src/main/kotlin/com/supremeai/ide/SupremeAIKnowledgePanel.kt`

```kotlin
package com.supremeai.ide

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTable
import com.intellij.ui.content.ContentFactory
import java.awt.BorderLayout
import javax.swing.JPanel
import javax.swing.JSplitPane
import javax.swing.table.AbstractTableModel

/**
 * Knowledge management panel for SupremeAI tool window
 */
class SupremeAIKnowledgePanel(private val project: Project) {
    
    private val knowledgeStore = com.supremeai.ide.learning.LocalKnowledgeStore.getInstance()
    private val knowledgeTable = JBTable(KnowledgeTableModel())
    private val detailPanel = KnowledgeDetailPanel()
    
    fun createComponent(): JPanel {
        val panel = JPanel(BorderLayout())
        
        // Create table with knowledge items
        val scrollPane = JBScrollPane(knowledgeTable)
        
        // Create split pane for table and details
        val splitPane = JSplitPane(
            JSplitPane.VERTICAL_SPLIT,
            scrollPane,
            detailPanel.createComponent()
        )
        splitPane.dividerLocation = 300
        
        panel.add(splitPane, BorderLayout.CENTER)
        
        // Add listeners
        knowledgeTable.selectionModel.addListSelectionListener { event ->
            if (!event.valueIsAdjusting) {
                val selectedRow = knowledgeTable.selectedRow
                if (selectedRow >= 0) {
                    val item = (knowledgeTable.model as KnowledgeTableModel).getItem(selectedRow)
                    detailPanel.showItem(item)
                }
            }
        }
        
        return panel
    }
    
    /**
     * Refresh knowledge table
     */
    fun refresh() {
        (knowledgeTable.model as KnowledgeTableModel).refresh()
        knowledgeTable.repaint()
    }
    
    /**
     * Table model for knowledge items
     */
    inner class KnowledgeTableModel : AbstractTableModel() {
        
        private var items = listOf<com.supremeai.ide.learning.KnowledgeItem>()
        
        init {
            refresh()
        }
        
        fun refresh() {
            items = knowledgeStore.getAll().sortedByDescending { it.metadata.capturedAt }
            fireTableDataChanged()
        }
        
        fun getItem(row: Int): com.supremeai.ide.learning.KnowledgeItem {
            return items[row]
        }
        
        override fun getRowCount(): Int = items.size
        
        override fun getColumnCount(): Int = 6
        
        override fun getColumnName(column: Int): String {
            return when (column) {
                0 -> "Type"
                1 -> "Provider"
                2 -> "Confidence"
                3 -> "Security"
                4 -> "Tags"
                5 -> "Captured"
                else -> ""
            }
        }
        
        override fun getValueAt(row: Int, column: Int): Any {
            val item = items[row]
            return when (column) {
                0 -> item.type.name
                1 -> item.provider
                2 -> String.format("%.0f%%", item.quality.confidence * 100)
                3 -> String.format("%.0f%%", item.quality.securityScore * 100)
                4 -> item.tags.take(3).joinToString(", ")
                5 -> item.metadata.capturedAt.toString().substring(0, 10)
                else -> ""
            }
        }
    }
}

/**
 * Detail panel for knowledge item
 */
class KnowledgeDetailPanel {
    
    private var currentItem: com.supremeai.ide.learning.KnowledgeItem? = null
    
    fun createComponent(): JPanel {
        // Implementation for detail panel
        return JPanel()
    }
    
    fun showItem(item: com.supremeai.ide.learning.KnowledgeItem) {
        currentItem = item
        // Update UI with item details
    }
}
```

### 11. Integrate with Existing Tool Window

**File:** `src/main/kotlin/com/supremeai/ide/SupremeAIToolWindowFactory.kt` (Update)

Add the following to the existing `createToolWindowContent` method:

```kotlin
// Add Knowledge tab
val knowledgePanel = SupremeAIKnowledgePanel(project)
val knowledgeContent = contentFactory.createContent(knowledgePanel.createComponent(), "Knowledge", false)
toolWindow.contentManager.addContent(knowledgeContent)
```

### 12. Add Configuration Options

**File:** `src/main/kotlin/com/supremeai/ide/SupremeAISettings.kt` (Update)

Add the following fields to the `SupremeAISettings` class:

```kotlin
// External AI Learning Settings
var enableExternalAILearning: Boolean = false
var autoCaptureAIGeneratedCode: Boolean = true
var shareKnowledgeWithCommunity: Boolean = false
var knowledgeQualityThreshold: Double = 0.7
var allowedAISources: MutableList<String> = mutableListOf(
    "ChatGPT",
    "Claude",
    "Gemini",
    "GitHub Copilot",
    "Other"
)
var knowledgeSyncFrequency: String = "realtime" // realtime, hourly, daily
```

### 13. Add Settings UI Components

**File:** `src/main/kotlin/com/supremeai/ide/SupremeAISettingsConfigurable.kt` (Update)

Add the following to the settings panel:

```kotlin
// External AI Learning Settings
val enableLearningField = JBCheckBox("Enable External AI Learning")
val autoCaptureField = JBCheckBox("Auto-capture AI-generated code")
val shareKnowledgeField = JBCheckBox("Share knowledge with community")
val qualityThresholdField = JSpinner(SpinnerNumberModel(0.7, 0.0, 1.0, 0.1))

// Add to settings panel
formBuilder.addComponent(JBLabel("External AI Learning Settings").apply {
    font = font.deriveFont(Font.BOLD)
})
formBuilder.addComponent(enableLearningField)
formBuilder.addComponent(autoCaptureField)
formBuilder.addComponent(shareKnowledgeField)
formBuilder.addLabeledComponent("Quality Threshold:", qualityThresholdField)
```

## Integration Points

### 1. Document Listener Integration

Update `UserCodeLearningProjectComponent.kt` to use the new detector:

```kotlin
import com.supremeai.ide.learning.ExternalAIDetector
import com.supremeai.ide.learning.KnowledgeCaptureService

class UserCodeLearningProjectComponent(private val project: Project) : 
    ProjectComponent,
    FileEditorManagerListener {
    
    private val detector = ExternalAIDetector(project)
    private val captureService = KnowledgeCaptureService.getInstance(project)
    
    override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        attachDocumentListenerIfNeeded(file)
    }
    
    private fun attachDocumentListenerIfNeeded(file: VirtualFile) {
        // Get editor for file
        val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return
        
        // Register AI detection listener
        detector.registerDocumentListener(editor) { detectionResult ->
            // Capture knowledge when AI-generated code is detected
            val context = CodeContext(
                filePath = file.path,
                language = file.fileType.name,
                project = project.name,
                lineStart = 0,
                lineEnd = 0
            )
            
            captureService.captureAIGeneratedCode(
                code = editor.document.text,
                context = context,
                detectionResult = detectionResult
            )
        }
    }
}
```

### 2. Clipboard Monitoring

Add clipboard listener to detect pasted AI-generated code:

```kotlin
import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.ClipboardOwner
import java.awt.datatransfer.Transferable

class AIClipboardMonitor(private val project: Project) : ClipboardOwner {
    
    private val detector = ExternalAIDetector(project)
    private val captureService = KnowledgeCaptureService.getInstance(project)
    private val clipboard: Clipboard = Toolkit.getDefaultToolkit().systemClipboard
    
    fun startMonitoring() {
        clipboard.addFlavorListener {
            checkClipboardContent()
        }
    }
    
    private fun checkClipboardContent() {
        try {
            val contents = clipboard.getContents(null)
            if (contents != null && contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                val content = contents.getTransferData(DataFlavor.stringFlavor) as? String
                if (!content.isNullOrBlank() && content.length > 50) {
                    val result = detector.checkPastedContent(content, null)
                    if (result.isAIGenerated) {
                        // Show notification to user
                        showCaptureNotification(result)
                    }
                }
            }
        } catch (e: Exception) {
            // Handle exception
        }
    }
    
    private fun showCaptureNotification(result: AIDetectionResult) {
        // Show notification asking user if they want to capture this AI-generated code
    }
    
    override fun lostOwnership(clipboard: Clipboard, contents: Transferable) {
        // Handle clipboard ownership loss
    }
}
```

## Testing Strategy

### Unit Tests

**File:** `src/test/kotlin/com/supremeai/ide/learning/ExternalAIDetectorTest.kt`

```kotlin
package com.supremeai.ide.learning

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class ExternalAIDetectorTest {
    
    private val detector = ExternalAIDetector(null)
    
    @Test
    fun `detect ChatGPT comment pattern`() {
        val code = "// ChatGPT: Here's a function to calculate fibonacci"
        val result = detector.checkCode(code)
        
        assertTrue(result.isAIGenerated)
        assertEquals("ChatGPT", result.provider)
        assertTrue(result.confidence > 0.7)
    }
    
    @Test
    fun `detect Claude thinking block`() {
        val code = """
            // <thinking>
            // This is a complex problem that requires careful consideration
            // </thinking>
            fun solve() {}
        """.trimIndent()
        
        val result = detector.checkCode(code)
        
        assertTrue(result.isAIGenerated)
        assertEquals("Claude", result.provider)
    }
    
    @Test
    fun `detect AI-generated code characteristics`() {
        val code = """
            // Explanation: This function calculates the factorial of a number
            // using recursion. The base case is when n is 0 or 1, in which case
            // we return 1. Otherwise, we multiply n by the factorial of n-1.
            fun factorial(n: Int): Int {
                return if (n <= 1) 1 else n * factorial(n - 1)
            }
        """.trimIndent()
        
        val result = detector.checkCode(code)
        
        assertTrue(result.isAIGenerated)
        assertTrue(result.characteristics.hasExplanatoryComments)
        assertTrue(result.characteristics.commentDensity > 0.1)
    }
    
    @Test
    fun `reject human-written code`() {
        val code = """
            fun add(a: Int, b: Int): Int {
                return a + b
            }
        """.trimIndent()
        
        val result = detector.checkCode(code)
        
        assertFalse(result.isAIGenerated)
    }
}
```

**File:** `src/test/kotlin/com/supremeai/ide/learning/KnowledgeCaptureServiceTest.kt`

```kotlin
package com.supremeai.ide.learning

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class KnowledgeCaptureServiceTest {
    
    private val captureService = KnowledgeCaptureService(null)
    
    @Test
    fun `capture AI-generated code`() {
        val code = "// ChatGPT: Here's a helper function\nfun helper() {}"
        val context = CodeContext(
            filePath = "/test/file.kt",
            language = "kotlin",
            project = "test-project",
            lineStart = 1,
            lineEnd = 2,
            userPrompt = "Create a helper function"
        )
        
        val detectionResult = AIDetectionResult(
            isAIGenerated = true,
            confidence = 0.9,
            provider = "ChatGPT",
            detections = emptyList(),
            characteristics = AICharacteristics(0.8),
            timestamp = System.currentTimeMillis()
        )
        
        val item = captureService.captureAIGeneratedCode(
            code = code,
            context = context,
            detectionResult = detectionResult
        )
        
        assertNotNull(item.id)
        assertEquals(KnowledgeType.CODE_SUGGESTION, item.type)
        assertEquals("ChatGPT", item.provider)
        assertEquals(code, item.response)
        assertFalse(item.synced)
    }
    
    @Test
    fun `capture bug fix`() {
        val originalCode = "fun add(a: Int, b: Int) = a - b"
        val fixedCode = "fun add(a: Int, b: Int) = a + b"
        val bugDescription = "Subtraction instead of addition"
        
        val context = CodeContext(
            filePath = "/test/file.kt",
            language = "kotlin",
            project = "test-project",
            lineStart = 1,
            lineEnd = 1
        )
        
        val item = captureService.captureBugFix(
            originalCode = originalCode,
            fixedCode = fixedCode,
            bugDescription = bugDescription,
            context = context
        )
        
        assertNotNull(item.id)
        assertEquals(KnowledgeType.BUG_FIX, item.type)
        assertEquals(originalCode, item.relatedCode)
        assertEquals(fixedCode, item.response)
    }
}
```

**File:** `src/test/kotlin/com/supremeai/ide/learning/SecurityAnalyzerTest.kt`

```kotlin
package com.supremeai.ide.learning

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class SecurityAnalyzerTest {
    
    @Test
    fun `detect hardcoded password`() {
        val code = "val password = \"secret123\""
        val result = SecurityAnalyzer.analyze(code)
        
        assertTrue(result.score < 1.0)
        assertTrue(result.vulnerabilities.any { it.type == "Hardcoded Secret" })
    }
    
    @Test
    fun `detect SQL injection`() {
        val code = "\"SELECT * FROM users WHERE id = \\$\{userId}\""
        val result = SecurityAnalyzer.analyze(code)
        
        assertTrue(result.vulnerabilities.any { it.type == "SQL Injection" })
        assertEquals("CRITICAL", result.vulnerabilities.first { it.type == "SQL Injection" }.severity)
    }
    
    @Test
    fun `safe code has high score`() {
        val code = """
            fun add(a: Int, b: Int): Int {
                return a + b
            }
        """.trimIndent()
        
        val result = SecurityAnalyzer.analyze(code)
        
        assertEquals(1.0, result.score)
        assertTrue(result.vulnerabilities.isEmpty())
    }
}
```

**File:** `src/test/kotlin/com/supremeai/ide/learning/PerformanceAnalyzerTest.kt`

```kotlin
package com.supremeai.ide.learning

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class PerformanceAnalyzerTest {
    
    @Test
    fun `detect string concatenation in loop`() {
        val code = """
            var result = \"\"
            for (i in 1..100) {
                result += i.toString()
            }
        """.trimIndent()
        
        val result = PerformanceAnalyzer.analyze(code)
        
        assertTrue(result.issues.any { it.type == "String Concatenation in Loop" })
        assertEquals("HIGH", result.issues.first { it.type == "String Concatenation in Loop" }.severity)
    }
    
    @Test
    fun `detect nested loops`() {
        val code = """
            for (i in 1..100) {
                for (j in 1..100) {
                    println(i * j)
                }
            }
        """.trimIndent()
        
        val result = PerformanceAnalyzer.analyze(code)
        
        assertTrue(result.issues.any { it.type == "Nested Loops" })
    }
    
    @Test
    fun `efficient code has high score`() {
        val code = """
            fun sum(numbers: List<Int>): Int {
                return numbers.sum()
            }
        """.trimIndent()
        
        val result = PerformanceAnalyzer.analyze(code)
        
        assertEquals(1.0, result.score)
        assertTrue(result.issues.isEmpty())
    }
}
```

**File:** `src/test/kotlin/com/supremeai/ide/learning/KnowledgeValidatorTest.kt`

```kotlin
package com.supremeai.ide.learning

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.time.Instant

class KnowledgeValidatorTest {
    
    @Test
    fun `validate good knowledge item`() {
        val item = KnowledgeItem(
            id = "test-id",
            type = KnowledgeType.CODE_SUGGESTION,
            provider = "ChatGPT",
            prompt = "Create a helper function",
            response = "fun helper() {}",
            context = KnowledgeContext(
                file = "/test/file.kt",
                language = "kotlin",
                project = "test",
                lineStart = 1,
                lineEnd = 1
            ),
            quality = KnowledgeQuality(
                confidence = 0.9,
                complexity = Complexity.LOW,
                securityScore = 0.9,
                performanceScore = 0.9
            ),
            feedback = null,
            tags = listOf("kotlin", "function"),
            metadata = KnowledgeMetadata(
                capturedAt = Instant.now(),
                capturedBy = "test",
                source = "test",
                version = "1.0"
            ),
            synced = false
        )
        
        val result = KnowledgeValidator.validate(item)
        
        assertTrue(result.isValid)
        assertTrue(result.reasons.isEmpty())
    }
    
    @Test
    fun `reject low confidence item`() {
        val item = KnowledgeItem(
            id = "test-id",
            type = KnowledgeType.CODE_SUGGESTION,
            provider = "ChatGPT",
            prompt = "Create a helper function",
            response = "fun helper() {}",
            context = KnowledgeContext(
                file = "/test/file.kt",
                language = "kotlin",
                project = "test",
                lineStart = 1,
                lineEnd = 1
            ),
            quality = KnowledgeQuality(
                confidence = 0.3, // Too low
                complexity = Complexity.LOW,
                securityScore = 0.9,
                performanceScore = 0.9
            ),
            feedback = null,
            tags = listOf("kotlin", "function"),
            metadata = KnowledgeMetadata(
                capturedAt = Instant.now(),
                capturedBy = "test",
                source = "test",
                version = "1.0"
            ),
            synced = false
        )
        
        val result = KnowledgeValidator.validate(item)
        
        assertFalse(result.isValid)
        assertTrue(result.reasons.any { it.contains("Confidence") })
    }
    
    @Test
    fun `reject empty response`() {
        val item = KnowledgeItem(
            id = "test-id",
            type = KnowledgeType.CODE_SUGGESTION,
            provider = "ChatGPT",
            prompt = "Create a helper function",
            response = "", // Empty
            context = KnowledgeContext(
                file = "/test/file.kt",
                language = "kotlin",
                project = "test",
                lineStart = 1,
                lineEnd = 1
            ),
            quality = KnowledgeQuality(
                confidence = 0.9,
                complexity = Complexity.LOW,
                securityScore = 0.9,
                performanceScore = 0.9
            ),
            feedback = null,
            tags = listOf("kotlin", "function"),
            metadata = KnowledgeMetadata(
                capturedAt = Instant.now(),
                capturedBy = "test",
                source = "test",
                version = "1.0"
            ),
            synced = false
        )
        
        val result = KnowledgeValidator.validate(item)
        
        assertFalse(result.isValid)
        assertTrue(result.reasons.any { it.contains("empty") })
    }
}
```

## Build Configuration Updates

### Update `build.gradle.kts`

Add dependencies for serialization:

```kotlin
dependencies {
    // ... existing dependencies ...
    
    // Kotlin serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    
    // JSON processing
    implementation("com.google.code.gson:gson:2.10.1")
    
    // HTTP client
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    
    // Logging
    implementation("org.slf4j:slf4j-api:2.0.9")
}
```

### Update `gradle.properties`

Add serialization configuration:

```properties
# Kotlin serialization
kotlin.code.style=official
kotlin.incremental.js=true
```

## Deployment Checklist

### Pre-Deployment
- [ ] All unit tests passing
- [ ] Integration tests completed
- [ ] Security audit completed
- [ ] Performance testing completed
- [ ] Documentation updated
- [ ] Migration guide created

### Deployment
- [ ] Version bumped to 1.3.0
- [ ] Release notes prepared
- [ ] Plugin signed
- [ ] Marketplace submission prepared
- [ ] Beta testing group notified

### Post-Deployment
- [ ] Monitor error rates
- [ ] Collect user feedback
- [ ] Track usage metrics
- [ ] Address reported issues
- [ ] Plan next iteration

## Migration Guide

### For Existing Users

1. **Update Plugin**: Install version 1.3.0 from marketplace
2. **Enable Learning**: Go to Settings → SupremeAI → Enable External AI Learning
3. **Configure Providers**: Select which AI sources to track
4. **Start Learning**: Plugin will automatically capture and validate knowledge
5. **Review Knowledge**: Access Knowledge tab in SupremeAI tool window

### Data Migration

- Existing learning data is preserved
- New knowledge items use enhanced schema
- Backward compatible with previous versions
- No data loss during upgrade

## Support

### Resources
- **Documentation**: See `EXTERNAL_AI_LEARNING_INTEGRATION.md`
- **API Reference**: See inline code documentation
- **Issue Tracker**: GitHub Issues
- **Support Email**: support@supremeai.com

### Troubleshooting

| Issue | Solution |
|-------|----------|
| Detection not working | Check that AI sources are enabled in settings |
| Knowledge not syncing | Verify API key and internet connection |
| Performance issues | Reduce sync frequency in settings |
| False positives | Adjust confidence threshold in settings |

---

*Implementation Guide Version: 1.0.0*  
*Last Updated: 2026-05-03*  
*Next Review: 2026-05-10*

**Ready for Implementation** ✅