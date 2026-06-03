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

class GroqProviderTest {MockWebServerpublic GroqProviderTest(MockWebServer mockWebServer, GroqProvider provider, ObjectMapper objectMapper) {
MockWebServer    this.mockWebServer = mockWebServer;
MockWebServer    this.provider = provider;
MockWebServer    this.objectMapper = objectMapper;
MockWebServer}






    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        String baseUrl = mockWebServer.url("/openai/v1/chat/completions").toString();
        provider = new GroqProvider("gsk-test-key");
        try {
            java.lang.reflect.Field field = GroqProvider.class.getSuperclass().getDeclaredField("baseUrl");
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
    void getName_shouldReturnGroq() {
        assertEquals("groq", provider.getName());
    }

    @Test
    void getCapabilities_shouldReturnNameAndModels() {
        Map<String, Object> caps = provider.getCapabilities();

        assertNotNull(caps);
        assertEquals("Groq", caps.get("name"));
        assertNotNull(caps.get("models"));
    }

    @Test
    void getCapabilities_shouldIncludeLlamaAndMixtral() {
        Map<String, Object> caps = provider.getCapabilities();
        String[] models = (String[]) caps.get("models");

        assertTrue(models.length >= 3);
        assertTrue(java.util.Arrays.asList(models).contains("llama2-70b-4096"));
        assertTrue(java.util.Arrays.asList(models).contains("mixtral-8x7b-32768"));
        assertTrue(java.util.Arrays.asList(models).contains("gemma-7b-it"));
    }

    @Test
    void createRequestBody_shouldUseMixtralModel() throws Exception {
        java.lang.reflect.Method method = GroqProvider.class.getDeclaredMethod("createRequestBody", String.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) method.invoke(provider, "Hello Groq");

        assertNotNull(body);
        assertEquals("mixtral-8x7b-32768", body.get("model"));
        assertNotNull(body.get("messages"));
    }

    @Test
    void extractResponse_shouldParseValidResponse() throws Exception {
        String jsonResponse = objectMapper.writeValueAsString(Map.of(
                "choices", java.util.List.of(
                        Map.of("message", Map.of("content", "Hello from Groq!"))
                )
        ));

        java.lang.reflect.Method method = GroqProvider.class.getDeclaredMethod("extractResponse", String.class);
        method.setAccessible(true);
        String result = (String) method.invoke(provider, jsonResponse);

        assertEquals("Hello from Groq!", result);
    }

    @Test
    void extractResponse_shouldReturnDefault_whenNoChoices() throws Exception {
        String jsonResponse = objectMapper.writeValueAsString(Map.of("choices", java.util.List.of()));

        java.lang.reflect.Method method = GroqProvider.class.getDeclaredMethod("extractResponse", String.class);
        method.setAccessible(true);
        String result = (String) method.invoke(provider, jsonResponse);

        assertEquals("No response from Groq.", result);
    }

    @Test
    void constructor_default_shouldUseEmptyKey() {
        GroqProvider p = new GroqProvider();
        assertEquals("groq", p.getName());
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
                        Map.of("message", Map.of("content", "Mocked Groq response"))
                )
        ));
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(responseBody)
                .addHeader("Content-Type", "application/json"));

        StepVerifier.create(provider.generate("test prompt"))
                .expectNext("Mocked Groq response")
                .verifyComplete();

        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("Bearer gsk-test-key", request.getHeader("Authorization"));
        assertEquals("POST", request.getMethod());
    }

    @Test
    void httpIntegration_shouldHandleHttpError() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("{\"error\": \"Internal server error\"}"));

        StepVerifier.create(provider.generate("test"))
                .expectError(RuntimeException.class)
                .verify();
    }
}
