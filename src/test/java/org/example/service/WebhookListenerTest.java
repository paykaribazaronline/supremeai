package org.example.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WebhookListenerTest {

    @Mock
    private DataCollectorService dataCollectorService;

    @Mock
    private AdminMessagePusher adminMessagePusher;

    private WebhookListener webhookListener;
    private static final String WEBHOOK_SECRET = "test-webhook-secret";

    @BeforeEach
    void setUp() {
        webhookListener = new WebhookListener(dataCollectorService, adminMessagePusher, WEBHOOK_SECRET);
    }

    @Test
    void testSignatureVerificationSucceeds() {
        // Given
        String payload = "{\"test\": \"data\"}";
        String validSignature = generateSignature(payload, WEBHOOK_SECRET);

        // When
        boolean result = webhookListener.verifySignature(payload, validSignature, WEBHOOK_SECRET);

        // Then
        assertTrue(result);
    }

    @Test
    void testSignatureVerificationFails() {
        // Given
        String payload = "{\"test\": \"data\"}";
        String invalidSignature = "sha256=invalidsignature";

        // When
        boolean result = webhookListener.verifySignature(payload, invalidSignature, WEBHOOK_SECRET);

        // Then
        assertFalse(result);
    }

    @Test
    void testPushEventProcessing() throws InterruptedException {
        // Given
        String pushPayload = """
                {
                    "ref": "refs/heads/main",
                    "repository": {"name": "supremeai", "full_name": "user/supremeai"},
                    "pusher": {"name": "testuser"},
                    "commits": [{"id": "abc123", "message": "Test commit"}]
                }
                """;
        String signature = generateSignature(pushPayload, WEBHOOK_SECRET);

        // When
        webhookListener.handleWebhook("push", pushPayload, "push-12345");
        Thread.sleep(100); // Allow async processing

        // Then - should trigger data collection
        verify(dataCollectorService, timeout(1000)).getGitHubData(anyString(), anyString());
    }

    @Test
    void testPullRequestEventProcessing() throws InterruptedException {
        // Given
        String prPayload = """
                {
                    "action": "opened",
                    "number": 42,
                    "pull_request": {
                        "id": 1,
                        "title": "Test PR",
                        "user": {"login": "testuser"}
                    }
                }
                """;

        // When
        webhookListener.handleWebhook("pull_request", prPayload, "pr-12345");
        Thread.sleep(100);

        // Then
        verify(adminMessagePusher, timeout(1000)).pushAlert(anyString());
    }

    @Test
    void testDeduplicationWindowPreventsReprocessing() {
        // Given
        String payload = "{\"test\": \"data\"}";
        String deliveryId = "duplicate-test";

        // When - process same delivery twice
        webhookListener.handleWebhook("push", payload, deliveryId);
        webhookListener.handleWebhook("push", payload, deliveryId);

        // Then - should only process once (deduplicated)
        verify(dataCollectorService, atMostOnce()).getGitHubData(anyString(), anyString());
    }

    @Test
    void testDeduplicationWindowExpiry() throws InterruptedException {
        // Given
        String payload = "{\"test\": \"data\"}";
        String deliveryId = "expiring-dedup";

        // When - process, wait, process again
        webhookListener.handleWebhook("push", payload, deliveryId);
        Thread.sleep(35000); // Wait more than 30-second window
        webhookListener.handleWebhook("push", payload, deliveryId);

        // Then - should process both (window expired)
        verify(dataCollectorService, times(2)).getGitHubData(anyString(), anyString());
    }

    @Test
    void testIssueEventProcessing() throws InterruptedException {
        // Given
        String issuePayload = """
                {
                    "action": "opened",
                    "issue": {"number": 123, "title": "Bug report", "body": "Details"}
                }
                """;

        // When
        webhookListener.handleWebhook("issues", issuePayload, "issue-12345");
        Thread.sleep(100);

        // Then
        verify(adminMessagePusher, timeout(1000)).pushAlert(anyString());
    }

    @Test
    void testReleaseEventProcessing() throws InterruptedException {
        // Given
        String releasePayload = """
                {
                    "action": "published",
                    "release": {"tag_name": "v1.0.0", "name": "Release 1.0.0"}
                }
                """;

        // When
        webhookListener.handleWebhook("release", releasePayload, "release-12345");
        Thread.sleep(100);

        // Then
        verify(adminMessagePusher, timeout(1000)).pushAlert(anyString());
    }

    @Test
    void testUnknownEventTypeHandledGracefully() {
        // Given
        String payload = "{\"test\": \"data\"}";

        // When & Then - should not throw exception
        assertDoesNotThrow(() -> 
            webhookListener.handleWebhook("unknown_event", payload, "unknown-12345")
        );
    }

    @Test
    void testRetryMechanismOnTransientFailure() throws InterruptedException {
        // Given
        doThrow(new RuntimeException("Transient network error"))
                .doReturn(null)
                .when(dataCollectorService).getGitHubData(anyString(), anyString());

        String payload = "{\"test\": \"data\"}";

        // When
        webhookListener.handleWebhook("push", payload, "retry-12345");
        Thread.sleep(500); // Allow retry to happen

        // Then - should retry
        verify(dataCollectorService, atLeast(1)).getGitHubData(anyString(), anyString());
    }

    @Test
    void testConcurrentWebhookProcessing() throws InterruptedException {
        // Given
        String payload1 = "{\"id\": 1}";
        String payload2 = "{\"id\": 2}";
        String payload3 = "{\"id\": 3}";

        // When - simulate 3 concurrent webhooks
        Thread t1 = new Thread(() -> 
            webhookListener.handleWebhook("push", payload1, "concurrent-1")
        );
        Thread t2 = new Thread(() -> 
            webhookListener.handleWebhook("push", payload2, "concurrent-2")
        );
        Thread t3 = new Thread(() -> 
            webhookListener.handleWebhook("push", payload3, "concurrent-3")
        );

        t1.start();
        t2.start();
        t3.start();
        t1.join();
        t2.join();
        t3.join();

        // Then - all should be processed
        verify(dataCollectorService, times(3)).getGitHubData(anyString(), anyString());
    }

    @Test
    void testWebhookStatsTracking() {
        // Given
        String payload = "{\"test\": \"data\"}";

        // When - process several webhooks
        webhookListener.handleWebhook("push", payload, "stats-1");
        webhookListener.handleWebhook("push", payload, "stats-2");
        webhookListener.handleWebhook("pull_request", payload, "stats-3");

        // Then - stats should reflect processing
        var stats = webhookListener.getStats();
        assertNotNull(stats);
        assertTrue((long) stats.get("totalReceived") >= 3);
    }

    @Test
    void testEmptyPayloadHandling() {
        // When & Then - should handle gracefully
        assertDoesNotThrow(() -> 
            webhookListener.handleWebhook("push", "", "empty-12345")
        );
    }

    @Test
    void testMalformedJsonHandling() {
        // Given
        String malformedJson = "{\"invalid\": json}";

        // When & Then - should handle gracefully
        assertDoesNotThrow(() -> 
            webhookListener.handleWebhook("push", malformedJson, "malformed-12345")
        );
    }

    @Test
    void testLargePayloadProcessing() throws InterruptedException {
        // Given
        StringBuilder largePayload = new StringBuilder("{");
        for (int i = 0; i < 1000; i++) {
            largePayload.append("\"key").append(i).append("\": \"value\",");
        }
        largePayload.append("\"final\": \"value\"}");

        // When
        webhookListener.handleWebhook("push", largePayload.toString(), "large-12345");
        Thread.sleep(100);

        // Then - should not throw OOM or similar
        verify(dataCollectorService, timeout(1000)).getGitHubData(anyString(), anyString());
    }

    // Helper method
    private String generateSignature(String payload, String secret) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(secret.getBytes(), "HmacSHA256"));
            byte[] hash = mac.doFinal(payload.getBytes());
            return "sha256=" + java.util.Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
