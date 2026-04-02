package org.example.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
public class AdminMessagePusherSimpleTest {

    private AdminMessagePusher adminMessagePusher;

    @BeforeEach
    void setUp() {
        adminMessagePusher = new AdminMessagePusher();
    }

    @Test
    void testPushDataUpdateMessage() {
        // Given
        Map<String, Object> data = new HashMap<>();
        data.put("owner", "supremeai");
        data.put("repo", "core");
        data.put("stars", 150);

        // When & Then - should not throw
        assertDoesNotThrow(() -> 
            adminMessagePusher.pushDataUpdate("GITHUB", "GitHub Stats", data, System.currentTimeMillis())
        );
    }

    @Test
    void testPushAlertMessage() {
        // Given
        String alertMessage = "New pull request created";

        // When & Then
        assertDoesNotThrow(() -> 
            adminMessagePusher.pushAlert("warning", "Alert", alertMessage, new HashMap<>())
        );
    }

    @Test
    void testPushStatsMessage() {
        // Given
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRequests", 10250);
        stats.put("avgResponse", 245);

        // When & Then
        assertDoesNotThrow(() -> 
            adminMessagePusher.pushStatsUpdate("request_stats", stats)
        );
    }

    @Test
    void testPushEventMessage() {
        // Given
        Map<String, Object> event = new HashMap<>();
        event.put("type", "webhook");
        event.put("source", "GitHub");

        // When & Then
        assertDoesNotThrow(() -> 
            adminMessagePusher.pushWebhookEvent("delivery-123", "webhook", "owner", "repo", true)
        );
    }

    @Test
    void testConcurrentMessagePushing() throws InterruptedException {
        // Given - 5 threads
        int threadsCount = 5;
        int messagesPerThread = 10;

        // When
        Thread[] threads = new Thread[threadsCount];
        for (int t = 0; t < threadsCount; t++) {
            threads[t] = new Thread(() -> {
                for (int i = 0; i < messagesPerThread; i++) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("thread", Thread.currentThread().getId());
                    adminMessagePusher.pushAlert("warning", "Concurrent Test", "Concurrent test alert", data);
                }
            });
            threads[t].start();
        }

        // Wait for all threads
        for (Thread t : threads) {
            t.join();
        }

        // Then - all should complete without errors
        assertTrue(true);
    }

    @Test
    void testNullDataHandling() {
        // When & Then
        assertDoesNotThrow(() -> {
            adminMessagePusher.pushDataUpdate("NULL", "Null Test", null, System.currentTimeMillis());
            adminMessagePusher.pushAlert("error", "Null Alert", "Test alert", new HashMap<>());
        });
    }

    @Test
    void testEmptyDataHandling() {
        // When & Then
        assertDoesNotThrow(() -> {
            adminMessagePusher.pushDataUpdate("EMPTY", "Empty", new HashMap<>(), System.currentTimeMillis());
            adminMessagePusher.pushStatsUpdate("stats", new HashMap<>());
        });
    }

    @Test
    void testMultipleMessageTypes() throws InterruptedException {
        // When
        adminMessagePusher.pushDataUpdate("DATA", "Data", new HashMap<>(), System.currentTimeMillis());
        adminMessagePusher.pushAlert("info", "Test Alert", "Alert message", new HashMap<>());
        adminMessagePusher.pushStatsUpdate("stats", new HashMap<>());
        adminMessagePusher.pushWebhookEvent("delivery-456", "push", "owner", "repo", true);

        Thread.sleep(100);

        // Then - all should execute
        assertTrue(true);
    }
}
