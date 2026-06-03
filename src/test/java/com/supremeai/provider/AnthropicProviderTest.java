package com.supremeai.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AnthropicProviderTest {MockWebServerpublic AnthropicProviderTest(MockWebServer mockWebServer, AnthropicProvider provider, ObjectMapper objectMapper) {
MockWebServer    this.mockWebServer = mockWebServer;
MockWebServer    this.provider = provider;
MockWebServer    this.objectMapper = objectMapper;
MockWebServer}






    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        String baseUrl = mockWebServer.url("/v1/messages").toString();
        provider = new AnthropicProvider("sk-ant-test-key");
        try {
            java.lang.reflect.Field field = AnthropicProvider.class.getSuperclass().getDeclaredField("baseUrl");
            field.setAccessible(true);
            field.set(provider, baseUrl);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        objectMapper = new ObjectMapper();
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void getName_shouldReturnAnthropic() {
        assertEquals("anthropic", provider.getName());
    }

    @Test
    void getCapabilities_shouldReturnNameAndModels() {
        Map<String, Object> caps = provider.getCapabilities();

        assertNotNull(caps);
        assertEquals("Anthropic", caps.get("name"));
        assertNotNull(caps.get("models"));
        // Accept either List or array
        Object modelsObj = caps.get("models");
        assertTrue(modelsObj instanceof java.util.List || modelsObj instanceof String[]);
    }

    @Test
    void getCapabilities_shouldIncludeClaudeModels() {
        Map<String, Object> caps = provider.getCapabilities();
        Object modelsObj = caps.get("models");

        @SuppressWarnings("unchecked")
        java.util.List<String> models = (modelsObj instanceof java.util.List)
            ? (java.util.List<String>) modelsObj
            : java.util.Arrays.asList((String[]) modelsObj);

        assertTrue(models.size() >= 2);
        assertTrue(models.contains("claude-3-opus-20240229"));
        assertTrue(models.contains("claude-3-sonnet-20240229"));
    }

    @Test
    void createRequestBody_shouldContainMessagesModelAndMaxTokens() throws Exception {
        java.lang.reflect.Method method = AnthropicProvider.class.getDeclaredMethod("createRequestBody", String.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) method.invoke(provider, "Hello Claude");

        assertNotNull(body);
        assertNotNull(body.get("messages"));
        assertEquals("claude-3-sonnet-20240229", body.get("model"));
        assertEquals(1024, body.get("max_tokens"));
    }

    @Test
    void createRequestBody_shouldWrapPromptAsUserMessage() throws Exception {
        java.lang.reflect.Method method = AnthropicProvider.class.getDeclaredMethod("createRequestBody", String.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) method.invoke(provider, "My prompt");

        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> messages = (java.util.List<Map<String, Object>>) body.get("messages");
        assertEquals(1, messages.size());
        assertEquals("user", messages.get(0).get("role"));
        assertEquals("My prompt", messages.get(0).get("content"));
    }

    @Test
    void extractResponse_shouldParseValidResponse() throws Exception {
        String jsonResponse = objectMapper.writeValueAsString(Map.of(
                "content", java.util.List.of(
                        Map.of("text", "Hello from Claude!")
                )
        ));

        java.lang.reflect.Method method = AnthropicProvider.class.getDeclaredMethod("extractResponse", String.class);
        method.setAccessible(true);
        String result = (String) method.invoke(provider, jsonResponse);

        assertEquals("Hello from Claude!", result);
    }

    @Test
    void extractResponse_shouldReturnDefault_whenContentEmpty() throws Exception {
        String jsonResponse = objectMapper.writeValueAsString(Map.of("content", java.util.List.of()));

        java.lang.reflect.Method method = AnthropicProvider.class.getDeclaredMethod("extractResponse", String.class);
        method.setAccessible(true);
        String result = (String) method.invoke(provider, jsonResponse);

        assertEquals("No response from Anthropic.", result);
    }

    @Test
    void extractResponse_shouldReturnDefault_whenContentNull() throws Exception {
        String jsonResponse = objectMapper.writeValueAsString(Map.of());

        java.lang.reflect.Method method = AnthropicProvider.class.getDeclaredMethod("extractResponse", String.class);
        method.setAccessible(true);
        String result = (String) method.invoke(provider, jsonResponse);

        assertEquals("No response from Anthropic.", result);
    }

    @Test
    void inheritance_shouldExtendAbstractHttpProvider() {
        assertTrue(provider instanceof AbstractHttpProvider);
        assertTrue(provider instanceof AIProvider);
    }

    @Test
    void httpIntegration_shouldReturnGeneratedText() throws Exception {
        String responseBody = objectMapper.writeValueAsString(Map.of(
                "content", java.util.List.of(
                        Map.of("text", "Mocked Anthropic response"))
        ));
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(responseBody)
                .addHeader("Content-Type", "application/json"));

        StepVerifier.create(provider.generate("test prompt"))
                .expectNext("Mocked Anthropic response")
                .verifyComplete();

        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("sk-ant-test-key", request.getHeader("x-api-key"));
        assertEquals("2023-06-01", request.getHeader("anthropic-version"));
        assertEquals("POST", request.getMethod());
    }

    @Test
    void httpIntegration_shouldHandleHttpError() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(429)
                .setBody("{\"error\": \"Rate limited\"}"));

        StepVerifier.create(provider.generate("test"))
                .expectError(RuntimeException.class)
                .verify();
    }
}
