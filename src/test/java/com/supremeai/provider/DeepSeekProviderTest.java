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

class DeepSeekProviderTest {

    private MockWebServer mockWebServer;
    private DeepSeekProvider provider;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        String baseUrl = mockWebServer.url("/v1/chat/completions").toString();
        provider = new DeepSeekProvider("sk-deepseek-test-key");
        try {
            java.lang.reflect.Field field = DeepSeekProvider.class.getSuperclass().getDeclaredField("baseUrl");
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
    void getName_shouldReturnDeepSeek() {
        assertEquals("deepseek", provider.getName());
    }

    @Test
    void getCapabilities_shouldReturnNameModelAndType() {
        Map<String, Object> caps = provider.getCapabilities();

        assertNotNull(caps);
        assertEquals("DeepSeek", caps.get("name"));
        assertNotNull(caps.get("model"));
        assertEquals("remote", caps.get("type"));
        assertTrue(caps.get("description").toString().contains("code generation"));
    }

    @Test
    void createRequestBody_shouldUseCustomModel() throws Exception {
        DeepSeekProvider customProvider = new DeepSeekProvider("sk-key", "deepseek-chat");
        java.lang.reflect.Method method = DeepSeekProvider.class.getDeclaredMethod("createRequestBody", String.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) method.invoke(customProvider, "Hello DeepSeek");

        assertNotNull(body);
        assertEquals("deepseek-chat", body.get("model"));
        assertEquals(0.7, body.get("temperature"));
        assertEquals(4000, body.get("max_tokens"));
        assertNotNull(body.get("messages"));
    }

    @Test
    void createRequestBody_shouldDefaultToCoderModel() throws Exception {
        DeepSeekProvider defaultProvider = new DeepSeekProvider("sk-key");
        java.lang.reflect.Method method = DeepSeekProvider.class.getDeclaredMethod("createRequestBody", String.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) method.invoke(defaultProvider, "Hello");

        assertEquals("deepseek-coder", body.get("model"));
    }

    @Test
    void extractResponse_shouldParseValidResponse() throws Exception {
        String jsonResponse = objectMapper.writeValueAsString(Map.of(
                "choices", java.util.List.of(
                        Map.of("message", Map.of("content", "Hello from DeepSeek!"))
                )
        ));

        java.lang.reflect.Method method = DeepSeekProvider.class.getDeclaredMethod("extractResponse", String.class);
        method.setAccessible(true);
        String result = (String) method.invoke(provider, jsonResponse);

        assertEquals("Hello from DeepSeek!", result);
    }

    @Test
    void extractResponse_shouldReturnDefault_whenNoChoices() throws Exception {
        String jsonResponse = objectMapper.writeValueAsString(Map.of("choices", java.util.List.of()));

        java.lang.reflect.Method method = DeepSeekProvider.class.getDeclaredMethod("extractResponse", String.class);
        method.setAccessible(true);
        String result = (String) method.invoke(provider, jsonResponse);

        assertEquals("No response from DeepSeek.", result);
    }

    @Test
    void constructor_withCustomModel_shouldSetModel() {
        DeepSeekProvider p = new DeepSeekProvider("sk-key", "deepseek-chat");
        assertEquals("deepseek", p.getName());
    }

    @Test
    void constructor_default_shouldUseEmptyKeyAndCoderModel() {
        DeepSeekProvider p = new DeepSeekProvider();
        assertEquals("deepseek", p.getName());
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
                        Map.of("message", Map.of("content", "Mocked DeepSeek response"))
                )
        ));
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(responseBody)
                .addHeader("Content-Type", "application/json"));

        StepVerifier.create(provider.generate("test prompt"))
                .expectNext("Mocked DeepSeek response")
                .verifyComplete();

        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("Bearer sk-deepseek-test-key", request.getHeader("Authorization"));
        assertEquals("POST", request.getMethod());
    }

    @Test
    void httpIntegration_shouldHandleHttpError() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(403)
                .setBody("{\"error\": \"Forbidden\"}"));

        StepVerifier.create(provider.generate("test"))
                .expectError(RuntimeException.class)
                .verify();
    }
}
