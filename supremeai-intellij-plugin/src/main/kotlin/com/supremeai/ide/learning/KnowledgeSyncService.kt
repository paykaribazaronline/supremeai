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