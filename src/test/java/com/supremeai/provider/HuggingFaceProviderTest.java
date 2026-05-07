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

class HuggingFaceProviderTest {

    private MockWebServer mockWebServer;
    private HuggingFaceProvider provider;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        String baseUrl = mockWebServer.url("/models/HuggingFaceH4/zephyr-7b-beta").toString();
        provider = new HuggingFaceProvider("hf-test-key");
        try {
            java.lang.reflect.Field field = HuggingFaceProvider.class.getSuperclass().getDeclaredField("baseUrl");
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
    void getName_shouldReturnHuggingFace() {
        assertEquals("huggingface", provider.getName());
    }

    @Test
    void getCapabilities_shouldReturnNameAndModels() {
        Map<String, Object> caps = provider.getCapabilities();

        assertNotNull(caps);
        assertEquals("HuggingFace", caps.get("name"));
        assertNotNull(caps.get("models"));
    }

    @Test
    void createRequestBody_shouldUseInputsFormat() throws Exception {
        java.lang.reflect.Method method = HuggingFaceProvider.class.getDeclaredMethod("createRequestBody", String.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) method.invoke(provider, "Hello HF");

        assertNotNull(body);
        assertEquals("Hello HF", body.get("inputs"));
        assertFalse(body.containsKey("messages"));
        assertNotNull(body.get("parameters"));
    }

    @Test
    void createRequestBody_shouldIncludeMaxNewTokens() throws Exception {
        java.lang.reflect.Method method = HuggingFaceProvider.class.getDeclaredMethod("createRequestBody", String.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) method.invoke(provider, "Hello");

        @SuppressWarnings("unchecked")
        Map<String, Object> params = (Map<String, Object>) body.get("parameters");
        assertEquals(512, params.get("max_new_tokens"));
    }

    @Test
    void extractResponse_shouldParseArrayResponse() throws Exception {
        String jsonResponse = objectMapper.writeValueAsString(
                java.util.List.of(Map.of("generated_text", "Hello from HuggingFace!"))
        );

        java.lang.reflect.Method method = HuggingFaceProvider.class.getDeclaredMethod("extractResponse", String.class);
        method.setAccessible(true);
        String result = (String) method.invoke(provider, jsonResponse);

        assertEquals("Hello from HuggingFace!", result);
    }

    @Test
    void extractResponse_shouldReturnDefault_whenArrayEmpty() throws Exception {
        String jsonResponse = objectMapper.writeValueAsString(java.util.List.of());

        java.lang.reflect.Method method = HuggingFaceProvider.class.getDeclaredMethod("extractResponse", String.class);
        method.setAccessible(true);
        String result = (String) method.invoke(provider, jsonResponse);

        assertEquals("No response from HuggingFace.", result);
    }

    @Test
    void extractResponse_shouldReturnDefault_whenGeneratedTextNull() throws Exception {
        java.util.Map<String, Object> responseMap = new java.util.HashMap<>();
        responseMap.put("generated_text", null);
        String jsonResponse = objectMapper.writeValueAsString(
                java.util.List.of(responseMap)
        );

        java.lang.reflect.Method method = HuggingFaceProvider.class.getDeclaredMethod("extractResponse", String.class);
        method.setAccessible(true);
        String result = (String) method.invoke(provider, jsonResponse);

        assertEquals("No response from HuggingFace.", result);
    }

    @Test
    void constructor_default_shouldUseEmptyKey() {
        HuggingFaceProvider p = new HuggingFaceProvider();
        assertEquals("huggingface", p.getName());
    }

    @Test
    void inheritance_shouldExtendAbstractHttpProvider() {
        assertTrue(provider instanceof AbstractHttpProvider);
        assertTrue(provider instanceof AIProvider);
    }

    @Test
    void httpIntegration_shouldReturnGeneratedText() throws Exception {
        String responseBody = objectMapper.writeValueAsString(
                java.util.List.of(Map.of("generated_text", "Mocked HF response"))
        );
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(responseBody)
                .addHeader("Content-Type", "application/json"));

        StepVerifier.create(provider.generate("test prompt"))
                .expectNext("Mocked HF response")
                .verifyComplete();

        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("Bearer hf-test-key", request.getHeader("Authorization"));
        assertEquals("POST", request.getMethod());
    }

    @Test
    void httpIntegration_shouldHandleHttpError() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(503)
                .setBody("{\"error\": \"Service unavailable\"}"));

        StepVerifier.create(provider.generate("test"))
                .expectError(RuntimeException.class)
                .verify();
    }
}
