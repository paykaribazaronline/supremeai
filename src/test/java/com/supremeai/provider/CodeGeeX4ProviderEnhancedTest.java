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

class CodeGeeX4ProviderEnhancedTest {MockWebServerpublic CodeGeeX4ProviderEnhancedTest(MockWebServer mockWebServer, CodeGeeX4Provider provider, ObjectMapper objectMapper) {
MockWebServer    this.mockWebServer = mockWebServer;
MockWebServer    this.provider = provider;
MockWebServer    this.objectMapper = objectMapper;
MockWebServer}






    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        String baseUrl = mockWebServer.url("/api/coding/paas/v4/chat/completions").toString();
        provider = new CodeGeeX4Provider("test-key");
        try {
            java.lang.reflect.Field field = CodeGeeX4Provider.class.getSuperclass().getDeclaredField("baseUrl");
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
    void getName_shouldReturnCodeGeeX4() {
        assertEquals("codegeex4", provider.getName());
    }

    @Test
    void getCapabilities_shouldReturnAllFields() {
        Map<String, Object> caps = provider.getCapabilities();

        assertNotNull(caps);
        assertEquals("CodeGeeX4 (智谱AI)", caps.get("name"));
        assertEquals("CodeGeeX4", caps.get("provider"));
        assertNotNull(caps.get("models"));
        assertNotNull(caps.get("freeTier"));
        assertNotNull(caps.get("pricing"));
        assertEquals("128K tokens", caps.get("context"));
        assertNotNull(caps.get("supports"));
        assertEquals("50+ programming languages", caps.get("languages"));
        assertNotNull(caps.get("baseUrl"));
    }

    @Test
    void getCapabilities_shouldListTwoModels() {
        Map<String, Object> caps = provider.getCapabilities();
        java.util.List<?> models = (java.util.List<?>) caps.get("models");

        assertEquals(2, models.size());
        assertTrue(models.contains("codegeex-4"));
        assertTrue(models.contains("codegeex-4-lite"));
    }

    @Test
    void getCapabilities_shouldIncludeCodeRelatedSupports() {
        Map<String, Object> caps = provider.getCapabilities();
        java.util.List<?> supports = (java.util.List<?>) caps.get("supports");

        assertTrue(supports.contains("code_generation"));
        assertTrue(supports.contains("code_completion"));
        assertTrue(supports.contains("code_review"));
        assertTrue(supports.contains("unit_test_generation"));
    }

    @Test
    void createRequestBody_shouldIncludeSystemMessage() throws Exception {
        java.lang.reflect.Method method = CodeGeeX4Provider.class.getDeclaredMethod("createRequestBody", String.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) method.invoke(provider, "Write a function");

        assertNotNull(body);
        assertEquals("codegeex-4", body.get("model"));
        assertEquals(0.7, body.get("temperature"));
        assertEquals(0.7, body.get("top_p"));
        assertEquals(4000, body.get("max_tokens"));
        assertEquals(false, body.get("stream"));

        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> messages = (java.util.List<Map<String, Object>>) body.get("messages");
        assertEquals(2, messages.size());
        assertEquals("system", messages.get(0).get("role"));
        assertTrue(messages.get(0).get("content").toString().contains("CodeGeeX"));
        assertEquals("user", messages.get(1).get("role"));
        assertEquals("Write a function", messages.get(1).get("content"));
    }

    @Test
    void createRequestBody_shouldUseCustomModel() throws Exception {
        CodeGeeX4Provider customProvider = new CodeGeeX4Provider("key", "codegeex-4-lite");
        java.lang.reflect.Method method = CodeGeeX4Provider.class.getDeclaredMethod("createRequestBody", String.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) method.invoke(customProvider, "Hello");

        assertEquals("codegeex-4-lite", body.get("model"));
    }

    @Test
    void extractResponse_shouldParseValidResponse() throws Exception {
        String jsonResponse = objectMapper.writeValueAsString(Map.of(
                "choices", java.util.List.of(
                        Map.of("message", Map.of("content", "Hello from CodeGeeX4!"))
                )
        ));

        java.lang.reflect.Method method = CodeGeeX4Provider.class.getDeclaredMethod("extractResponse", String.class);
        method.setAccessible(true);
        String result = (String) method.invoke(provider, jsonResponse);

        assertEquals("Hello from CodeGeeX4!", result);
    }

    @Test
    void extractResponse_shouldThrowOnErrorResponse() throws Exception {
        String jsonResponse = objectMapper.writeValueAsString(Map.of(
                "error", "Invalid API key"
        ));

        java.lang.reflect.Method method = CodeGeeX4Provider.class.getDeclaredMethod("extractResponse", String.class);
        method.setAccessible(true);

        Exception exception = assertThrows(Exception.class, () -> method.invoke(provider, jsonResponse));
        assertTrue(exception.getCause().getMessage().contains("CodeGeeX4 Error"));
    }

    @Test
    void extractResponse_shouldReturnDefault_whenNoChoices() throws Exception {
        String jsonResponse = objectMapper.writeValueAsString(Map.of("choices", java.util.List.of()));

        java.lang.reflect.Method method = CodeGeeX4Provider.class.getDeclaredMethod("extractResponse", String.class);
        method.setAccessible(true);
        String result = (String) method.invoke(provider, jsonResponse);

        assertEquals("No response from CodeGeeX4.", result);
    }

    @Test
    void extractResponse_shouldHandleNullContent() throws Exception {
        java.util.Map<String, Object> messageMap = new java.util.HashMap<>();
        messageMap.put("content", null);
        String jsonResponse = objectMapper.writeValueAsString(Map.of(
                "choices", java.util.List.of(
                        Map.of("message", messageMap)
                )
        ));

        java.lang.reflect.Method method = CodeGeeX4Provider.class.getDeclaredMethod("extractResponse", String.class);
        method.setAccessible(true);
        String result = (String) method.invoke(provider, jsonResponse);

        assertEquals("No content in response", result);
    }

    @Test
    void constructor_default_shouldUseEmptyKeyAndDefaultModel() {
        CodeGeeX4Provider p = new CodeGeeX4Provider();
        assertEquals("codegeex4", p.getName());
    }

    @Test
    void constructor_withApiKey_shouldSetName() {
        CodeGeeX4Provider p = new CodeGeeX4Provider("my-key");
        assertEquals("codegeex4", p.getName());
    }

    @Test
    void constructor_withCustomModel_shouldAcceptModel() {
        CodeGeeX4Provider p = new CodeGeeX4Provider("key", "codegeex-4-lite");
        assertEquals("codegeex4", p.getName());
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
                        Map.of("message", Map.of("content", "Mocked CodeGeeX4 response"))
                )
        ));
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(responseBody)
                .addHeader("Content-Type", "application/json"));

        StepVerifier.create(provider.generate("test prompt"))
                .expectNext("Mocked CodeGeeX4 response")
                .verifyComplete();

        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("Bearer test-key", request.getHeader("Authorization"));
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
