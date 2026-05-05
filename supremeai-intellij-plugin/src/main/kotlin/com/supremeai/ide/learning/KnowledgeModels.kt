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