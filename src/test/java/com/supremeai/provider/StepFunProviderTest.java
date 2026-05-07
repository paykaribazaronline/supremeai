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

class StepFunProviderTest {

    private MockWebServer mockWebServer;
    private StepFunProvider provider;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        String baseUrl = mockWebServer.url("/v1/chat/completions").toString();
        provider = new StepFunProvider("sf-test-key");
        try {
            java.lang.reflect.Field field = StepFunProvider.class.getSuperclass().getDeclaredField("baseUrl");
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
    void getName_shouldReturnStepFun() {
        assertEquals("stepfun", provider.getName());
    }

    @Test
    void getCapabilities_shouldReturnNameAndModels() {
        Map<String, Object> caps = provider.getCapabilities();

        assertNotNull(caps);
        assertEquals("StepFun (阶跃星辰)", caps.get("name"));
        assertEquals("StepFun", caps.get("provider"));
        assertNotNull(caps.get("models"));
    }

    @Test
    void getCapabilities_shouldIncludeFreeTierAndRateLimit() {
        Map<String, Object> caps = provider.getCapabilities();

        assertNotNull(caps.get("freeTier"));
        assertNotNull(caps.get("rateLimit"));
        assertNotNull(caps.get("supports"));
        assertNotNull(caps.get("languages"));
    }

    @Test
    void getCapabilities_shouldListThreeModels() {
        Map<String, Object> caps = provider.getCapabilities();
        java.util.List<?> models = (java.util.List<?>) caps.get("models");

        assertEquals(3, models.size());
        assertTrue(models.contains("step-3.5-flash"));
        assertTrue(models.contains("step-3.5-pro"));
        assertTrue(models.contains("step-1"));
    }

    @Test
    void createRequestBody_shouldUseDefaultFlashModel() throws Exception {
        java.lang.reflect.Method method = StepFunProvider.class.getDeclaredMethod("createRequestBody", String.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) method.invoke(provider, "Hello StepFun");

        assertNotNull(body);
        assertEquals("step-3.5-flash", body.get("model"));
        assertNotNull(body.get("messages"));
    }

    @Test
    void createRequestBody_shouldUseCustomModel() throws Exception {
        StepFunProvider customProvider = new StepFunProvider("sf-key", "step-3.5-pro");
        java.lang.reflect.Method method = StepFunProvider.class.getDeclaredMethod("createRequestBody", String.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) method.invoke(customProvider, "Hello");

        assertEquals("step-3.5-pro", body.get("model"));
    }

    @Test
    void extractResponse_shouldParseValidResponse() throws Exception {
        String jsonResponse = objectMapper.writeValueAsString(Map.of(
                "choices", java.util.List.of(
                        Map.of("message", Map.of("content", "Hello from StepFun!"))
                )
        ));

        java.lang.reflect.Method method = StepFunProvider.class.getDeclaredMethod("extractResponse", String.class);
        method.setAccessible(true);
        String result = (String) method.invoke(provider, jsonResponse);

        assertEquals("Hello from StepFun!", result);
    }

    @Test
    void extractResponse_shouldReturnDefault_whenNoChoices() throws Exception {
        String jsonResponse = objectMapper.writeValueAsString(Map.of("choices", java.util.List.of()));

        java.lang.reflect.Method method = StepFunProvider.class.getDeclaredMethod("extractResponse", String.class);
        method.setAccessible(true);
        String result = (String) method.invoke(provider, jsonResponse);

        assertEquals("No response from StepFun.", result);
    }

    @Test
    void constructor_default_shouldUseEmptyKey() {
        StepFunProvider p = new StepFunProvider();
        assertEquals("stepfun", p.getName());
    }

    @Test
    void constructor_withApiKey_shouldSetName() {
        StepFunProvider p = new StepFunProvider("sf-my-key");
        assertEquals("stepfun", p.getName());
    }

    @Test
    void constructor_withCustomModel_shouldAcceptModel() {
        StepFunProvider p = new StepFunProvider("sf-key", "step-1");
        assertEquals("stepfun", p.getName());
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
                        Map.of("message", Map.of("content", "Mocked StepFun response"))
                )
        ));
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(responseBody)
                .addHeader("Content-Type", "application/json"));

        StepVerifier.create(provider.generate("test prompt"))
                .expectNext("Mocked StepFun response")
                .verifyComplete();

        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("Bearer sf-test-key", request.getHeader("Authorization"));
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
