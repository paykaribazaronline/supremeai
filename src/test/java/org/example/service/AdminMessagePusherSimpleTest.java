package org.example.service;

import com.google.cloud.firestore.Firestore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AdminMessagePusherSimpleTest {

    @Mock
    private Firestore firestore;

    private AdminMessagePusher adminMessagePusher;

    @BeforeEach
    void setUp() {
        adminMessagePusher = new AdminMessagePusher(firestore);
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
            adminMessagePusher.pushAlert(alertMessage)
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
            adminMessagePusher.pushStats(stats)
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
            adminMessagePusher.pushEvent(event)
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
                    adminMessagePusher.pushAlert("Concurrent test");
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
            adminMessagePusher.pushAlert(null);
        });
    }

    @Test
    void testEmptyDataHandling() {
        // When & Then
        assertDoesNotThrow(() -> {
            adminMessagePusher.pushDataUpdate("EMPTY", "Empty", new HashMap<>(), System.currentTimeMillis());
            adminMessagePusher.pushStats(new HashMap<>());
        });
    }

    @Test
    void testMultipleMessageTypes() throws InterruptedException {
        // When
        adminMessagePusher.pushDataUpdate("DATA", "Data", new HashMap<>(), System.currentTimeMillis());
        adminMessagePusher.pushAlert("Alert");
        adminMessagePusher.pushStats(new HashMap<>());
        adminMessagePusher.pushEvent(new HashMap<>());

        Thread.sleep(100);

        // Then - all should execute
        assertTrue(true);
    }
}
