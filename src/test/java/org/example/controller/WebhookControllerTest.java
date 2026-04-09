package org.example.controller;

import org.example.service.WebhookListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.util.concurrent.TimeUnit;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("integration")
public class WebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WebhookListener webhookListener;

    private String testPayload;
    private String validSignature;
    private static final String TEST_SECRET = "test-webhook-secret";

    @BeforeEach
    void setUp() throws Exception {
        testPayload = """
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

        // Generate valid HMAC signature
        validSignature = generateHmacSignature(testPayload, TEST_SECRET);
        
        doNothing().when(webhookListener).handleWebhook(any(String.class), any(String.class), any(String.class));
    }

    private String generateHmacSignature(String payload, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
        mac.init(secretKey);
        byte[] hash = mac.doFinal(payload.getBytes());
        return "sha256=" + Base64.getEncoder().encodeToString(hash);
    }

    @Test
    void testWebhookReceivedWithValidSignature() throws Exception {
        // When & Then
        mockMvc.perform(post("/webhook/github")
                .contentType(MediaType.APPLICATION_JSON)
                .content(testPayload)
                .header("X-Hub-Signature-256", validSignature)
                .header("X-GitHub-Event", "pull_request")
                .header("X-GitHub-Delivery", "12345-67890"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message", containsString("received")))
                .andExpect(jsonPath("$.deliveryId", is("12345-67890")));
    }

    @Test
    void testWebhookRejectedWithInvalidSignature() throws Exception {
        // When & Then
        mockMvc.perform(post("/webhook/github")
                .contentType(MediaType.APPLICATION_JSON)
                .content(testPayload)
                .header("X-Hub-Signature-256", "sha256=invalidsignature")
                .header("X-GitHub-Event", "pull_request")
                .header("X-GitHub-Delivery", "12345-67890"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", containsString("Invalid signature")));
    }

    @Test
    void testWebhookRejectedWithMissingSignature() throws Exception {
        // When & Then
        mockMvc.perform(post("/webhook/github")
                .contentType(MediaType.APPLICATION_JSON)
                .content(testPayload)
                .header("X-GitHub-Event", "pull_request")
                .header("X-GitHub-Delivery", "12345-67890"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("signature")));
    }

    @Test
    void testWebhookPushEventProcessing() throws Exception {
        // Given
        String pushPayload = """
                {
                    "ref": "refs/heads/main",
                    "repository": {"name": "supremeai", "full_name": "user/supremeai"},
                    "pusher": {"name": "testuser"},
                    "commits": [{"id": "abc123", "message": "Test commit"}]
                }
                """;
        String signature = generateHmacSignature(pushPayload, TEST_SECRET);

        // When & Then
        mockMvc.perform(post("/webhook/github")
                .contentType(MediaType.APPLICATION_JSON)
                .content(pushPayload)
                .header("X-Hub-Signature-256", signature)
                .header("X-GitHub-Event", "push")
                .header("X-GitHub-Delivery", "push-12345"))
                .andExpect(status().isAccepted());

        verify(webhookListener).handleWebhook("push", pushPayload, "push-12345");
    }

    @Test
    void testWebhookPullRequestEventProcessing() throws Exception {
        // When & Then
        mockMvc.perform(post("/webhook/github")
                .contentType(MediaType.APPLICATION_JSON)
                .content(testPayload)
                .header("X-Hub-Signature-256", validSignature)
                .header("X-GitHub-Event", "pull_request")
                .header("X-GitHub-Delivery", "pr-12345"))
                .andExpect(status().isAccepted());

        verify(webhookListener).handleWebhook("pull_request", testPayload, "pr-12345");
    }

    @Test
    void testWebhookIssueEventProcessing() throws Exception {
        // Given
        String issuePayload = """
                {
                    "action": "opened",
                    "issue": {"number": 123, "title": "Bug report", "body": "Details"}
                }
                """;
        String signature = generateHmacSignature(issuePayload, TEST_SECRET);

        // When & Then
        mockMvc.perform(post("/webhook/github")
                .contentType(MediaType.APPLICATION_JSON)
                .content(issuePayload)
                .header("X-Hub-Signature-256", signature)
                .header("X-GitHub-Event", "issues")
                .header("X-GitHub-Delivery", "issue-12345"))
                .andExpect(status().isAccepted());

        verify(webhookListener).handleWebhook("issues", issuePayload, "issue-12345");
    }

    @Test
    void testWebhookReleaseEventProcessing() throws Exception {
        // Given
        String releasePayload = """
                {
                    "action": "published",
                    "release": {"tag_name": "v1.0.0", "name": "Release 1.0.0"}
                }
                """;
        String signature = generateHmacSignature(releasePayload, TEST_SECRET);

        // When & Then
        mockMvc.perform(post("/webhook/github")
                .contentType(MediaType.APPLICATION_JSON)
                .content(releasePayload)
                .header("X-Hub-Signature-256", signature)
                .header("X-GitHub-Event", "release")
                .header("X-GitHub-Delivery", "release-12345"))
                .andExpect(status().isAccepted());

        verify(webhookListener).handleWebhook("release", releasePayload, "release-12345");
    }

    @Test
    void testWebhookStatsEndpoint() throws Exception {
        // When & Then
        mockMvc.perform(get("/webhook/github/stats")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalReceived").exists())
                .andExpect(jsonPath("$.data.totalProcessed").exists())
                .andExpect(jsonPath("$.data.deduplicationRate").exists());
    }

    @Test
    void testWebhookEmptyPayloadRejected() throws Exception {
        // When & Then
        mockMvc.perform(post("/webhook/github")
                .contentType(MediaType.APPLICATION_JSON)
                .content("")
                .header("X-GitHub-Event", "push")
                .header("X-GitHub-Delivery", "12345"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testWebhookConcurrentDeliveries() throws Exception {
        // Given - 3 different deliveries with different IDs
        // Generate signatures for testing
        String sig = generateHmacSignature(testPayload, TEST_SECRET);

        // When & Then - all should be accepted
        for (int i = 1; i <= 3; i++) {
            mockMvc.perform(post("/webhook/github")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(testPayload)
                    .header("X-Hub-Signature-256", sig)
                    .header("X-GitHub-Event", "pull_request")
                    .header("X-GitHub-Delivery", "concurrent-" + i))
                    .andExpect(status().isAccepted());
        }
    }

    @Test
    void testWebhookDeduplicationWindow() throws Exception {
        // Same delivery ID sent twice - second should be rejected
        String signature = generateHmacSignature(testPayload, TEST_SECRET);
        String deliveryId = "duplicate-test-12345";

        // First delivery - accepted
        mockMvc.perform(post("/webhook/github")
                .contentType(MediaType.APPLICATION_JSON)
                .content(testPayload)
                .header("X-Hub-Signature-256", signature)
                .header("X-GitHub-Event", "push")
                .header("X-GitHub-Delivery", deliveryId))
                .andExpect(status().isAccepted());

        // Second delivery with same ID - should be rejected (duplicate)
        mockMvc.perform(post("/webhook/github")
                .contentType(MediaType.APPLICATION_JSON)
                .content(testPayload)
                .header("X-Hub-Signature-256", signature)
                .header("X-GitHub-Event", "push")
                .header("X-GitHub-Delivery", deliveryId))
                .andExpect(status().isConflict());
    }
}
