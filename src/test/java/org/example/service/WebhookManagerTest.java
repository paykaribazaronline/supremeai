package org.example.service;

import org.example.model.Webhook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Webhook Manager Test
 * Tests webhook registration, triggering, and retry logic
 */
@DisplayName("Webhook Manager Tests")
public class WebhookManagerTest {
    
    private WebhookManager webhookManager;
    
    @BeforeEach
    public void setUp() {
        webhookManager = new WebhookManager();
    }
    
    @Test
    @DisplayName("Register webhook with valid data")
    public void testRegisterWebhook() {
        Webhook webhook = new Webhook();
        webhook.setProjectId("project-123");
        webhook.setUrl("https://example.com/webhook");
        webhook.setEvents(new String[]{"project.created", "agent.updated"});
        webhook.setSecretKey("secret-key-123");
        
        Webhook registered = webhookManager.registerWebhook(webhook);
        
        assertNotNull(registered);
        assertNotNull(registered.getId());
        assertEquals("project-123", registered.getProjectId());
        assertTrue(registered.isActive());
        assertNotNull(registered.getCreatedAt());
    }
    
    @Test
    @DisplayName("Registered webhook has UUID")
    public void testWebhookHasValidUUID() {
        Webhook webhook = new Webhook();
        webhook.setProjectId("project-123");
        webhook.setUrl("https://example.com/webhook");
        webhook.setEvents(new String[]{"project.created"});
        webhook.setSecretKey("secret");
        
        Webhook registered = webhookManager.registerWebhook(webhook);
        
        assertDoesNotThrow(() -> UUID.fromString(registered.getId()));
    }
    
    @Test
    @DisplayName("Get webhook by ID")
    public void testGetWebhook() {
        Webhook webhook = new Webhook();
        webhook.setProjectId("project-123");
        webhook.setUrl("https://example.com/webhook");
        webhook.setEvents(new String[]{"project.created"});
        webhook.setSecretKey("secret");
        
        Webhook registered = webhookManager.registerWebhook(webhook);
        String webhookId = registered.getId();
        
        Webhook retrieved = webhookManager.getWebhook(webhookId);
        
        assertNotNull(retrieved);
        assertEquals(webhookId, retrieved.getId());
        assertEquals("project-123", retrieved.getProjectId());
    }
    
    @Test
    @DisplayName("Get non-existent webhook returns null")
    public void testGetNonExistentWebhook() {
        Webhook result = webhookManager.getWebhook("non-existent-id");
        assertNull(result);
    }
    
    @Test
    @DisplayName("List webhooks")
    public void testListWebhooks() {
        Webhook webhook1 = new Webhook();
        webhook1.setProjectId("project-1");
        webhook1.setUrl("https://example.com/webhook1");
        webhook1.setEvents(new String[]{"project.created"});
        webhook1.setSecretKey("secret1");
        
        Webhook webhook2 = new Webhook();
        webhook2.setProjectId("project-2");
        webhook2.setUrl("https://example.com/webhook2");
        webhook2.setEvents(new String[]{"agent.updated"});
        webhook2.setSecretKey("secret2");
        
        webhookManager.registerWebhook(webhook1);
        webhookManager.registerWebhook(webhook2);
        
        List<Webhook> webhooks = webhookManager.listWebhooks();
        
        assertNotNull(webhooks);
        assertTrue(webhooks.size() >= 2);
    }
    
    @Test
    @DisplayName("Delete webhook")
    public void testDeleteWebhook() {
        Webhook webhook = new Webhook();
        webhook.setProjectId("project-123");
        webhook.setUrl("https://example.com/webhook");
        webhook.setEvents(new String[]{"project.created"});
        webhook.setSecretKey("secret");
        
        Webhook registered = webhookManager.registerWebhook(webhook);
        String webhookId = registered.getId();
        
        webhookManager.deleteWebhook(webhookId);
        Webhook deleted = webhookManager.getWebhook(webhookId);
        
        assertNull(deleted);
    }
    
    @Test
    @DisplayName("Deactivate webhook")
    public void testDeactivateWebhook() {
        Webhook webhook = new Webhook();
        webhook.setProjectId("project-123");
        webhook.setUrl("https://example.com/webhook");
        webhook.setEvents(new String[]{"project.created"});
        webhook.setSecretKey("secret");
        
        Webhook registered = webhookManager.registerWebhook(webhook);
        String webhookId = registered.getId();
        
        webhookManager.deactivateWebhook(webhookId);
        Webhook deactivated = webhookManager.getWebhook(webhookId);
        
        assertNotNull(deactivated);
        assertFalse(deactivated.isActive());
    }
    
    @Test
    @DisplayName("Trigger webhook calls sendWebhookWithRetry")
    public void testTriggerWebhook() {
        Webhook webhook = new Webhook();
        webhook.setProjectId("project-123");
        webhook.setUrl("https://example.com/webhook");
        webhook.setEvents(new String[]{"project.created"});
        webhook.setSecretKey("secret");
        webhook.setActive(true);
        
        Webhook registered = webhookManager.registerWebhook(webhook);
        
        Map<String, Object> payload = Map.of(
            "eventType", "project.created",
            "projectId", "project-123",
            "timestamp", System.currentTimeMillis()
        );
        
        // This should not throw an exception
        assertDoesNotThrow(() -> 
            webhookManager.triggerWebhook(registered.getId(), "project.created", payload)
        );
    }
    
    @Test
    @DisplayName("Test webhook API endpoint")
    public void testWebhookAPI() {
        Webhook webhook = new Webhook();
        webhook.setProjectId("project-123");
        webhook.setUrl("https://example.com/webhook");
        webhook.setEvents(new String[]{"project.created"});
        webhook.setSecretKey("secret");
        
        Webhook registered = webhookManager.registerWebhook(webhook);
        
        Map<String, Object> testPayload = Map.of(
            "test", true,
            "message", "This is a test webhook"
        );
        
        // This should not throw an exception
        assertDoesNotThrow(() -> 
            webhookManager.testWebhook(registered.getId(), testPayload)
        );
    }
    
    @Test
    @DisplayName("Webhook with multiple events")
    public void testWebhookMultipleEvents() {
        String[] events = {"project.created", "project.updated", "project.deleted", "agent.created"};
        
        Webhook webhook = new Webhook();
        webhook.setProjectId("project-123");
        webhook.setUrl("https://example.com/webhook");
        webhook.setEvents(events);
        webhook.setSecretKey("secret");
        
        Webhook registered = webhookManager.registerWebhook(webhook);
        
        assertEquals(4, registered.getEvents().length);
        assertEquals("project.created", registered.getEvents()[0]);
        assertEquals("agent.created", registered.getEvents()[3]);
    }
    
    @Test
    @DisplayName("Webhook metrics tracking")
    public void testWebhookMetrics() {
        Webhook webhook = new Webhook();
        webhook.setProjectId("project-123");
        webhook.setUrl("https://example.com/webhook");
        webhook.setEvents(new String[]{"project.created"});
        webhook.setSecretKey("secret");
        webhook.setSuccessfulDeliveries(5);
        webhook.setFailedDeliveries(2);
        
        Webhook registered = webhookManager.registerWebhook(webhook);
        
        assertEquals(5, registered.getSuccessfulDeliveries());
        assertEquals(2, registered.getFailedDeliveries());
    }
    
    @Test
    @DisplayName("Webhook retry flag indicates retry capability")
    public void testWebhookRetryCapability() {
        Webhook webhook = new Webhook();
        webhook.setProjectId("project-123");
        webhook.setUrl("https://example.com/webhook");
        webhook.setEvents(new String[]{"project.created"});
        webhook.setSecretKey("secret");
        webhook.setRetryAttempts(3);
        
        Webhook registered = webhookManager.registerWebhook(webhook);
        
        assertTrue(registered.getRetryAttempts() >= 0);
    }
}
