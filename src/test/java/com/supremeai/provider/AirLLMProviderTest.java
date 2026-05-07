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

class AirLLMProviderTest {

    private MockWebServer mockWebServer;
    private AirLLMProvider provider;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        String baseUrl = mockWebServer.url("/v1/chat/completions").toString();
        provider = new AirLLMProvider(baseUrl, "airllm-test-key", "mistralai/Mistral-7B-Instruct-v0.3");
        objectMapper = new ObjectMapper();
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void getName_shouldReturnAirLLM() {
        assertEquals("airllm", provider.getName());
    }

    @Test
    void getCapabilities_shouldReturnNameModelAndEndpoint() {
        Map<String, Object> caps = provider.getCapabilities();

        assertNotNull(caps);
        assertEquals("AirLLM", caps.get("name"));
        assertNotNull(caps.get("model"));
        assertEquals("remote", caps.get("type"));
        assertNotNull(caps.get("endpoint"));
        assertTrue(caps.get("description").toString().contains("adaptive compression"));
    }

    @Test
    void createRequestBody_shouldIncludeTemperatureAndMaxTokens() throws Exception {
        java.lang.reflect.Method method = AirLLMProvider.class.getDeclaredMethod("createRequestBody", String.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) method.invoke(provider, "Hello AirLLM");

        assertNotNull(body);
        assertEquals(0.7, body.get("temperature"));
        assertEquals(2048, body.get("max_tokens"));
        assertNotNull(body.get("messages"));
    }

    @Test
    void extractResponse_shouldParseValidResponse() throws Exception {
        String jsonResponse = objectMapper.writeValueAsString(Map.of(
                "choices", java.util.List.of(
                        Map.of("message", Map.of("content", "Hello from AirLLM!"))
                )
        ));

        java.lang.reflect.Method method = AirLLMProvider.class.getDeclaredMethod("extractResponse", String.class);
        method.setAccessible(true);
        String result = (String) method.invoke(provider, jsonResponse);

        assertEquals("Hello from AirLLM!", result);
    }

    @Test
    void extractResponse_shouldReturnDefault_whenNoChoices() throws Exception {
        String jsonResponse = objectMapper.writeValueAsString(Map.of("choices", java.util.List.of()));

        java.lang.reflect.Method method = AirLLMProvider.class.getDeclaredMethod("extractResponse", String.class);
        method.setAccessible(true);
        String result = (String) method.invoke(provider, jsonResponse);

        assertEquals("No response from AirLLM.", result);
    }

    @Test
    void addAuthHeaders_shouldAddBearerToken_whenKeyProvided() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(objectMapper.writeValueAsString(Map.of(
                        "choices", java.util.List.of(
                                Map.of("message", Map.of("content", "response"))
                        )
                )))
                .addHeader("Content-Type", "application/json"));

        provider.generate("test").block();

        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("Bearer airllm-test-key", request.getHeader("Authorization"));
    }

    @Test
    void addAuthHeaders_shouldNotAddBearerToken_whenKeyEmpty() throws Exception {
        AirLLMProvider noAuthProvider = new AirLLMProvider(
                mockWebServer.url("/v1/chat/completions").toString(), "", "model");
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(objectMapper.writeValueAsString(Map.of(
                        "choices", java.util.List.of(
                                Map.of("message", Map.of("content", "response"))
                        )
                )))
                .addHeader("Content-Type", "application/json"));

        noAuthProvider.generate("test").block();

        RecordedRequest request = mockWebServer.takeRequest();
        assertNull(request.getHeader("Authorization"));
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
                        Map.of("message", Map.of("content", "Mocked AirLLM response"))
                )
        ));
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(responseBody)
                .addHeader("Content-Type", "application/json"));

        StepVerifier.create(provider.generate("test prompt"))
                .expectNext("Mocked AirLLM response")
                .verifyComplete();
    }

    @Test
    void httpIntegration_shouldHandleHttpError() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("{\"error\": \"Server error\"}"));

        StepVerifier.create(provider.generate("test"))
                .expectError(RuntimeException.class)
                .verify();
    }
}
