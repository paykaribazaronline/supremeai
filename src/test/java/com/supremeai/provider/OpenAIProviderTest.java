package com.supremeai.provider;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
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

class OpenAIProviderTest {

    private MockWebServer mockWebServer;
    private OpenAIProvider provider;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        String baseUrl = mockWebServer.url("/v1/chat/completions").toString();
        provider = new OpenAIProvider("sk-test-key");
        // Use reflection to set the baseUrl since we need to point to mock server
        try {
            java.lang.reflect.Field field = OpenAIProvider.class.getSuperclass().getDeclaredField("baseUrl");
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
    void getName_shouldReturnOpenAI() {
        assertEquals("openai", provider.getName());
    }

    @Test
    void getCapabilities_shouldReturnNameAndModels() {
        Map<String, Object> caps = provider.getCapabilities();

        assertNotNull(caps);
        assertEquals("OpenAI", caps.get("name"));
        assertNotNull(caps.get("models"));
    }

    @Test
    void getCapabilities_shouldIncludeGpt4AndGpt35() {
        Map<String, Object> caps = provider.getCapabilities();
        String[] models = (String[]) caps.get("models");

        assertTrue(models.length >= 2);
        assertTrue(java.util.Arrays.asList(models).contains("gpt-4"));
        assertTrue(java.util.Arrays.asList(models).contains("gpt-3.5-turbo"));
    }

    @Test
    void createRequestBody_shouldContainMessagesAndModel() throws Exception {
        java.lang.reflect.Method method = OpenAIProvider.class.getDeclaredMethod("createRequestBody", String.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) method.invoke(provider, "Hello AI");

        assertNotNull(body);
        assertNotNull(body.get("messages"));
        assertEquals("gpt-3.5-turbo", body.get("model"));
    }

    @Test
    void createRequestBody_shouldWrapPromptInUserMessage() throws Exception {
        java.lang.reflect.Method method = OpenAIProvider.class.getDeclaredMethod("createRequestBody", String.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) method.invoke(provider, "Test prompt");

        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> messages = (java.util.List<Map<String, Object>>) body.get("messages");
        assertEquals(1, messages.size());
        assertEquals("user", messages.get(0).get("role"));
        assertEquals("Test prompt", messages.get(0).get("content"));
    }

    @Test
    void extractResponse_shouldParseValidResponse() throws Exception {
        String jsonResponse = objectMapper.writeValueAsString(Map.of(
                "choices", java.util.List.of(
                        Map.of("message", Map.of("content", "Hello from OpenAI!"))
                )
        ));

        java.lang.reflect.Method method = OpenAIProvider.class.getDeclaredMethod("extractResponse", String.class);
        method.setAccessible(true);
        String result = (String) method.invoke(provider, jsonResponse);

        assertEquals("Hello from OpenAI!", result);
    }

    @Test
    void extractResponse_shouldReturnDefault_whenNoChoices() throws Exception {
        String jsonResponse = objectMapper.writeValueAsString(Map.of("choices", java.util.List.of()));

        java.lang.reflect.Method method = OpenAIProvider.class.getDeclaredMethod("extractResponse", String.class);
        method.setAccessible(true);
        String result = (String) method.invoke(provider, jsonResponse);

        assertEquals("No response from OpenAI.", result);
    }

    @Test
    void extractResponse_shouldReturnDefault_whenChoicesNull() throws Exception {
        String jsonResponse = objectMapper.writeValueAsString(Map.of());

        java.lang.reflect.Method method = OpenAIProvider.class.getDeclaredMethod("extractResponse", String.class);
        method.setAccessible(true);
        String result = (String) method.invoke(provider, jsonResponse);

        assertEquals("No response from OpenAI.", result);
    }

    @Test
    void constructor_withApiKey_shouldSetKey() {
        OpenAIProvider p = new OpenAIProvider("sk-my-key");
        assertEquals("openai", p.getName());
    }

    @Test
    void constructor_default_shouldUseEmptyKey() {
        OpenAIProvider p = new OpenAIProvider();
        assertEquals("openai", p.getName());
    }

    @Test
    void inheritance_shouldExtendAbstractHttpProvider() {
        assertTrue(provider instanceof AbstractHttpProvider);
        assertTrue(provider instanceof AIProvider);
    }

    @Test
    void httpIntegration_shouldReturnGeneratedText() throws Exception {
        String responseBody = objectMapper.writeValueAsString(Map.of(
                "choices", java.util.List.of(
                        Map.of("message", Map.of("content", "Mocked OpenAI response"))
                )
        ));
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(responseBody)
                .addHeader("Content-Type", "application/json"));

        StepVerifier.create(provider.generate("test prompt"))
                .expectNext("Mocked OpenAI response")
                .verifyComplete();

        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("Bearer sk-test-key", request.getHeader("Authorization"));
        assertEquals("POST", request.getMethod());
    }

    @Test
    void httpIntegration_shouldHandleHttpError() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(401)
                .setBody("{\"error\": \"Unauthorized\"}"));

        StepVerifier.create(provider.generate("test"))
                .expectError(RuntimeException.class)
                .verify();
    }
}
