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

class GeminiProviderTest {

    private MockWebServer mockWebServer;
    private GeminiProvider provider;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        String baseUrl = mockWebServer.url("/v1beta/models/gemini-1.5-flash:generateContent").toString();
        provider = new GeminiProvider("AIzaSy-test-key");
        try {
            java.lang.reflect.Field field = GeminiProvider.class.getSuperclass().getDeclaredField("baseUrl");
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
    void getName_shouldReturnGemini() {
        assertEquals("gemini", provider.getName());
    }

    @Test
    void getCapabilities_shouldReturnNameAndModels() {
        Map<String, Object> caps = provider.getCapabilities();

        assertNotNull(caps);
        assertEquals("Google Gemini", caps.get("name"));
        assertNotNull(caps.get("models"));
    }

    @Test
    void getCapabilities_shouldIncludeFlashAndPro() {
        Map<String, Object> caps = provider.getCapabilities();
        String[] models = (String[]) caps.get("models");

        assertTrue(models.length >= 2);
        assertTrue(java.util.Arrays.asList(models).contains("gemini-1.5-flash"));
        assertTrue(java.util.Arrays.asList(models).contains("gemini-1.5-pro"));
    }

    @Test
    void createRequestBody_shouldUseGeminiFormat() throws Exception {
        java.lang.reflect.Method method = GeminiProvider.class.getDeclaredMethod("createRequestBody", String.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) method.invoke(provider, "Hello Gemini");

        assertNotNull(body);
        assertNotNull(body.get("contents"));
        assertFalse(body.containsKey("messages"));
    }

    @Test
    void createRequestBody_shouldWrapPromptInParts() throws Exception {
        java.lang.reflect.Method method = GeminiProvider.class.getDeclaredMethod("createRequestBody", String.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) method.invoke(provider, "Test prompt");

        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> contents = (java.util.List<Map<String, Object>>) body.get("contents");
        assertEquals(1, contents.size());
        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> parts = (java.util.List<Map<String, Object>>) contents.get(0).get("parts");
        assertEquals(1, parts.size());
        assertEquals("Test prompt", parts.get(0).get("text"));
    }

    @Test
    void extractResponse_shouldParseValidResponse() throws Exception {
        String jsonResponse = objectMapper.writeValueAsString(Map.of(
                "candidates", java.util.List.of(
                        Map.of("content", Map.of(
                                "parts", java.util.List.of(Map.of("text", "Hello from Gemini!"))
                        ))
                )
        ));

        java.lang.reflect.Method method = GeminiProvider.class.getDeclaredMethod("extractResponse", String.class);
        method.setAccessible(true);
        String result = (String) method.invoke(provider, jsonResponse);

        assertEquals("Hello from Gemini!", result);
    }

    @Test
    void extractResponse_shouldReturnDefault_whenNoCandidates() throws Exception {
        String jsonResponse = objectMapper.writeValueAsString(Map.of("candidates", java.util.List.of()));

        java.lang.reflect.Method method = GeminiProvider.class.getDeclaredMethod("extractResponse", String.class);
        method.setAccessible(true);
        String result = (String) method.invoke(provider, jsonResponse);

        assertEquals("No response from Gemini.", result);
    }

    @Test
    void extractResponse_shouldReturnDefault_whenCandidatesNull() throws Exception {
        String jsonResponse = objectMapper.writeValueAsString(Map.of());

        java.lang.reflect.Method method = GeminiProvider.class.getDeclaredMethod("extractResponse", String.class);
        method.setAccessible(true);
        String result = (String) method.invoke(provider, jsonResponse);

        assertEquals("No response from Gemini.", result);
    }

    @Test
    void extractResponse_shouldReturnDefault_whenContentNull() throws Exception {
        java.util.Map<String, Object> contentMap = new java.util.HashMap<>();
        contentMap.put("content", null);
        String jsonResponse = objectMapper.writeValueAsString(Map.of(
                "candidates", java.util.List.of(contentMap)
        ));

        java.lang.reflect.Method method = GeminiProvider.class.getDeclaredMethod("extractResponse", String.class);
        method.setAccessible(true);
        String result = (String) method.invoke(provider, jsonResponse);

        assertEquals("No response from Gemini.", result);
    }

    @Test
    void extractResponse_shouldReturnDefault_whenPartsEmpty() throws Exception {
        String jsonResponse = objectMapper.writeValueAsString(Map.of(
                "candidates", java.util.List.of(
                        Map.of("content", Map.of("parts", java.util.List.of()))
                )
        ));

        java.lang.reflect.Method method = GeminiProvider.class.getDeclaredMethod("extractResponse", String.class);
        method.setAccessible(true);
        String result = (String) method.invoke(provider, jsonResponse);

        assertEquals("No response from Gemini.", result);
    }

    @Test
    void inheritance_shouldExtendAbstractHttpProvider() {
        assertTrue(provider instanceof AbstractHttpProvider);
        assertTrue(provider instanceof AIProvider);
    }

    @Test
    void httpIntegration_shouldAppendApiKeyAsQueryParam() throws Exception {
        String responseBody = objectMapper.writeValueAsString(Map.of(
                "candidates", java.util.List.of(
                        Map.of("content", Map.of(
                                "parts", java.util.List.of(Map.of("text", "Mocked Gemini response"))
                        ))
                )
        ));
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(responseBody)
                .addHeader("Content-Type", "application/json"));

        StepVerifier.create(provider.generate("test prompt"))
                .expectNext("Mocked Gemini response")
                .verifyComplete();

        RecordedRequest request = mockWebServer.takeRequest();
        assertTrue(request.getPath().contains("key=AIzaSy-test-key"));
        assertEquals("POST", request.getMethod());
    }

    @Test
    void httpIntegration_shouldHandleHttpError() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(400)
                .setBody("{\"error\": \"Bad request\"}"));

        StepVerifier.create(provider.generate("test"))
                .expectError(RuntimeException.class)
                .verify();
    }
}
