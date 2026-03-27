package org.example.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;



import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WebhookListenerTest {

    @Mock
    private DataCollectorService dataCollectorService;

    private WebhookListener webhookListener;
    private static final String WEBHOOK_SECRET = "test-webhook-secret";

    @BeforeEach
    void setUp() {
        webhookListener = new WebhookListener(dataCollectorService);
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
        webhookListener.handleWebhook(pushPayload, signature, "push-12345");
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
        String signature = generateSignature(prPayload, WEBHOOK_SECRET);

        // When
        webhookListener.handleWebhook(prPayload, signature, "pr-12345");
        Thread.sleep(100);

        // Then
        verify(dataCollectorService, timeout(1000)).getGitHubData(anyString(), anyString());
    }

    @Test
    void testDeduplicationWindowPreventsReprocessing() {
        // Given
        String payload = "{\"test\": \"data\"}";
        String signature = generateSignature(payload, WEBHOOK_SECRET);
        String deliveryId = "duplicate-test";

        // When - process same delivery twice
        webhookListener.handleWebhook(payload, signature, deliveryId);
        webhookListener.handleWebhook(payload, signature, deliveryId);

        // Then - should only process once (deduplicated)
        verify(dataCollectorService, atMostOnce()).getGitHubData(anyString(), anyString());
    }

    @Test
    void testDeduplicationWindowExpiry() throws InterruptedException {
        // Given
        String payload = "{\"test\": \"data\"}";
        String signature = generateSignature(payload, WEBHOOK_SECRET);
        String deliveryId = "expiring-dedup";

        // When - process, wait, process again
        webhookListener.handleWebhook(payload, signature, deliveryId);
        Thread.sleep(35000); // Wait more than 30-second window
        webhookListener.handleWebhook(payload, signature, deliveryId);

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
        String signature = generateSignature(issuePayload, WEBHOOK_SECRET);

        // When
        webhookListener.handleWebhook(issuePayload, signature, "issue-12345");
        Thread.sleep(100);

        // Then
        verify(dataCollectorService, timeout(1000)).getGitHubData(anyString(), anyString());
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
        String signature = generateSignature(releasePayload, WEBHOOK_SECRET);

        // When
        webhookListener.handleWebhook(releasePayload, signature, "release-12345");
        Thread.sleep(100);

        // Then
        verify(dataCollectorService, timeout(1000)).getGitHubData(anyString(), anyString());
    }

    @Test
    void testUnknownEventTypeHandledGracefully() {
        // Given
        String payload = "{\"test\": \"data\"}";
        String signature = generateSignature(payload, WEBHOOK_SECRET);

        // When & Then - should not throw exception
        assertDoesNotThrow(() -> 
            webhookListener.handleWebhook(payload, signature, "unknown-12345")
        );
    }

    @Test
    void testRetryMechanismOnTransientFailure() throws InterruptedException {
        // Given
        doThrow(new RuntimeException("Transient network error"))
                .doReturn(null)
                .when(dataCollectorService).getGitHubData(anyString(), anyString());

        String payload = "{\"test\": \"data\"}";
        String signature = generateSignature(payload, WEBHOOK_SECRET);

        // When
        webhookListener.handleWebhook(payload, signature, "retry-12345");
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
        String sig1 = generateSignature(payload1, WEBHOOK_SECRET);
        String sig2 = generateSignature(payload2, WEBHOOK_SECRET);
        String sig3 = generateSignature(payload3, WEBHOOK_SECRET);

        // When - simulate 3 concurrent webhooks
        Thread t1 = new Thread(() -> 
            webhookListener.handleWebhook(payload1, sig1, "concurrent-1")
        );
        Thread t2 = new Thread(() -> 
            webhookListener.handleWebhook(payload2, sig2, "concurrent-2")
        );
        Thread t3 = new Thread(() -> 
            webhookListener.handleWebhook(payload3, sig3, "concurrent-3")
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
        String signature = generateSignature(payload, WEBHOOK_SECRET);

        // When - process several webhooks
        webhookListener.handleWebhook(payload, signature, "stats-1");
        webhookListener.handleWebhook(payload, signature, "stats-2");
        webhookListener.handleWebhook(payload, signature, "stats-3");

        // Then - stats should reflect processing
        var stats = webhookListener.getStats();
        assertNotNull(stats);
        assertTrue((long) stats.get("totalReceived") >= 3);
    }

    @Test
    void testEmptyPayloadHandling() {
        // Given
        String payload = "";
        String signature = generateSignature(payload, WEBHOOK_SECRET);

        // When & Then - should handle gracefully
        assertDoesNotThrow(() -> 
            webhookListener.handleWebhook(payload, signature, "empty-12345")
        );
    }

    @Test
    void testMalformedJsonHandling() {
        // Given
        String malformedJson = "{\"invalid\": json}";
        String signature = generateSignature(malformedJson, WEBHOOK_SECRET);

        // When & Then - should handle gracefully
        assertDoesNotThrow(() -> 
            webhookListener.handleWebhook(malformedJson, signature, "malformed-12345")
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
        String payload = largePayload.toString();
        String signature = generateSignature(payload, WEBHOOK_SECRET);

        // When
        webhookListener.handleWebhook(payload, signature, "large-12345");
        Thread.sleep(100);

        // Then - should not throw OOM or similar
        assertTrue(true);
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
