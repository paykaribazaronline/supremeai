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

class KimiProviderTest {

    private MockWebServer mockWebServer;
    private KimiProvider provider;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        String baseUrl = mockWebServer.url("/v1/chat/completions").toString();
        provider = new KimiProvider("kimi-test-key");
        try {
            java.lang.reflect.Field field = KimiProvider.class.getSuperclass().getDeclaredField("baseUrl");
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
    void getName_shouldReturnKimi() {
        assertEquals("kimi", provider.getName());
    }

    @Test
    void getCapabilities_shouldReturnNameModelAndDescription() {
        Map<String, Object> caps = provider.getCapabilities();

        assertNotNull(caps);
        assertEquals("Kimi (Moonshot AI)", caps.get("name"));
        assertNotNull(caps.get("model"));
        assertEquals("remote", caps.get("type"));
        assertTrue(caps.get("description").toString().contains("128k"));
    }

    @Test
    void createRequestBody_shouldUse128kModel() throws Exception {
        java.lang.reflect.Method method = KimiProvider.class.getDeclaredMethod("createRequestBody", String.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) method.invoke(provider, "Hello Kimi");

        assertNotNull(body);
        assertEquals("moonshot-v1-128k", body.get("model"));
        assertEquals(0.7, body.get("temperature"));
        assertNotNull(body.get("messages"));
    }

    @Test
    void createRequestBody_shouldUseCustomModel() throws Exception {
        KimiProvider customProvider = new KimiProvider("key", "moonshot-v1-32k");
        java.lang.reflect.Method method = KimiProvider.class.getDeclaredMethod("createRequestBody", String.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) method.invoke(customProvider, "Hello");

        assertEquals("moonshot-v1-32k", body.get("model"));
    }

    @Test
    void extractResponse_shouldParseValidResponse() throws Exception {
        String jsonResponse = objectMapper.writeValueAsString(Map.of(
                "choices", java.util.List.of(
                        Map.of("message", Map.of("content", "Hello from Kimi!"))
                )
        ));

        java.lang.reflect.Method method = KimiProvider.class.getDeclaredMethod("extractResponse", String.class);
        method.setAccessible(true);
        String result = (String) method.invoke(provider, jsonResponse);

        assertEquals("Hello from Kimi!", result);
    }

    @Test
    void extractResponse_shouldReturnDefault_whenNoChoices() throws Exception {
        String jsonResponse = objectMapper.writeValueAsString(Map.of("choices", java.util.List.of()));

        java.lang.reflect.Method method = KimiProvider.class.getDeclaredMethod("extractResponse", String.class);
        method.setAccessible(true);
        String result = (String) method.invoke(provider, jsonResponse);

        assertEquals("No response from Kimi.", result);
    }

    @Test
    void constructor_default_shouldUseEmptyKey() {
        KimiProvider p = new KimiProvider();
        assertEquals("kimi", p.getName());
    }

    @Test
    void constructor_withCustomModel_shouldAcceptModel() {
        KimiProvider p = new KimiProvider("key", "moonshot-v1-8k");
        assertEquals("kimi", p.getName());
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
                        Map.of("message", Map.of("content", "Mocked Kimi response"))
                )
        ));
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(responseBody)
                .addHeader("Content-Type", "application/json"));

        StepVerifier.create(provider.generate("test prompt"))
                .expectNext("Mocked Kimi response")
                .verifyComplete();

        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("Bearer kimi-test-key", request.getHeader("Authorization"));
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
