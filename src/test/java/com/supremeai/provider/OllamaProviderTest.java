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

class OllamaProviderTest {

    private MockWebServer mockWebServer;
    private OllamaProvider provider;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        String baseUrl = mockWebServer.url("/v1/chat/completions").toString();
        provider = new OllamaProvider("dummy-key");
        try {
            java.lang.reflect.Field field = OllamaProvider.class.getSuperclass().getDeclaredField("baseUrl");
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
    void getName_shouldReturnOllama() {
        assertEquals("ollama", provider.getName());
    }

    @Test
    void getCapabilities_shouldReturnNameAndOfflineFlag() {
        Map<String, Object> caps = provider.getCapabilities();

        assertNotNull(caps);
        assertEquals("Ollama (Local)", caps.get("name"));
        assertNotNull(caps.get("models"));
        assertEquals(true, caps.get("offline"));
        assertEquals(true, caps.get("free"));
        assertEquals("http://localhost:11434", caps.get("endpoint"));
    }

    @Test
    void createRequestBody_shouldDisableStreaming() throws Exception {
        java.lang.reflect.Method method = OllamaProvider.class.getDeclaredMethod("createRequestBody", String.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) method.invoke(provider, "Hello Ollama");

        assertNotNull(body);
        assertEquals(false, body.get("stream"));
        assertNotNull(body.get("messages"));
        assertNotNull(body.get("model"));
    }

    @Test
    void createRequestBody_shouldUseCustomModel() throws Exception {
        OllamaProvider customProvider = new OllamaProvider("key", "llama3");
        java.lang.reflect.Method method = OllamaProvider.class.getDeclaredMethod("createRequestBody", String.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) method.invoke(customProvider, "Hello");

        assertEquals("llama3", body.get("model"));
    }

    @Test
    void extractResponse_shouldParseValidResponse() throws Exception {
        String jsonResponse = objectMapper.writeValueAsString(Map.of(
                "choices", java.util.List.of(
                        Map.of("message", Map.of("content", "Hello from Ollama!"))
                )
        ));

        java.lang.reflect.Method method = OllamaProvider.class.getDeclaredMethod("extractResponse", String.class);
        method.setAccessible(true);
        String result = (String) method.invoke(provider, jsonResponse);

        assertEquals("Hello from Ollama!", result);
    }

    @Test
    void extractResponse_shouldReturnDefault_whenNoChoices() throws Exception {
        String jsonResponse = objectMapper.writeValueAsString(Map.of("choices", java.util.List.of()));

        java.lang.reflect.Method method = OllamaProvider.class.getDeclaredMethod("extractResponse", String.class);
        method.setAccessible(true);
        String result = (String) method.invoke(provider, jsonResponse);

        assertEquals("No response from Ollama.", result);
    }

    @Test
    void constructor_default_shouldUseEmptyKey() {
        OllamaProvider p = new OllamaProvider();
        assertEquals("ollama", p.getName());
    }

    @Test
    void constructor_withCustomModel_shouldAcceptModel() {
        OllamaProvider p = new OllamaProvider("key", "mistral");
        assertEquals("ollama", p.getName());
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
                        Map.of("message", Map.of("content", "Mocked Ollama response"))
                )
        ));
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(responseBody)
                .addHeader("Content-Type", "application/json"));

        StepVerifier.create(provider.generate("test prompt"))
                .expectNext("Mocked Ollama response")
                .verifyComplete();

        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("POST", request.getMethod());
    }

    @Test
    void httpIntegration_shouldHandleHttpError() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(404)
                .setBody("{\"error\": \"Model not found\"}"));

        StepVerifier.create(provider.generate("test"))
                .expectError(RuntimeException.class)
                .verify();
    }
}
