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