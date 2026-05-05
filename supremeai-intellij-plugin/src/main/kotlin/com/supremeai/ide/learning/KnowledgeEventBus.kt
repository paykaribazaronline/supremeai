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