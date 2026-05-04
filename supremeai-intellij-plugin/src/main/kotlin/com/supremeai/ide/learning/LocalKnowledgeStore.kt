package com.supremeai.ide.learning

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

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