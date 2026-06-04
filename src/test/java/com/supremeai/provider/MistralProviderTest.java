package com.supremeai.provider;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

class MistralProviderTest {

  private MockWebServer mockWebServer;
  private MistralProvider provider;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() throws IOException {
    mockWebServer = new MockWebServer();
    mockWebServer.start();
    String baseUrl = mockWebServer.url("/v1/chat/completions").toString();
    provider = new MistralProvider("mistral-test-key");
    try {
      java.lang.reflect.Field field =
          MistralProvider.class.getSuperclass().getDeclaredField("baseUrl");
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
  void getName_shouldReturnMistral() {
    assertEquals("mistral", provider.getName());
  }

  @Test
  void getCapabilities_shouldReturnNameModelAndDescription() {
    Map<String, Object> caps = provider.getCapabilities();

    assertNotNull(caps);
    assertEquals("Mistral AI", caps.get("name"));
    assertNotNull(caps.get("model"));
    assertEquals("remote", caps.get("type"));
    assertTrue(caps.get("description").toString().contains("High-performance"));
  }

  @Test
  void createRequestBody_shouldUseLatestModel() throws Exception {
    java.lang.reflect.Method method =
        MistralProvider.class.getDeclaredMethod("createRequestBody", String.class);
    method.setAccessible(true);
    @SuppressWarnings("unchecked")
    Map<String, Object> body = (Map<String, Object>) method.invoke(provider, "Hello Mistral");

    assertNotNull(body);
    assertEquals("mistral-large-latest", body.get("model"));
    assertEquals(0.7, body.get("temperature"));
    assertNotNull(body.get("messages"));
  }

  @Test
  void createRequestBody_shouldUseCustomModel() throws Exception {
    MistralProvider customProvider = new MistralProvider("key", "mistral-small-latest");
    java.lang.reflect.Method method =
        MistralProvider.class.getDeclaredMethod("createRequestBody", String.class);
    method.setAccessible(true);
    @SuppressWarnings("unchecked")
    Map<String, Object> body = (Map<String, Object>) method.invoke(customProvider, "Hello");

    assertEquals("mistral-small-latest", body.get("model"));
  }

  @Test
  void extractResponse_shouldParseValidResponse() throws Exception {
    String jsonResponse =
        objectMapper.writeValueAsString(
            Map.of(
                "choices",
                java.util.List.of(Map.of("message", Map.of("content", "Hello from Mistral!")))));

    java.lang.reflect.Method method =
        MistralProvider.class.getDeclaredMethod("extractResponse", String.class);
    method.setAccessible(true);
    String result = (String) method.invoke(provider, jsonResponse);

    assertEquals("Hello from Mistral!", result);
  }

  @Test
  void extractResponse_shouldReturnDefault_whenNoChoices() throws Exception {
    String jsonResponse = objectMapper.writeValueAsString(Map.of("choices", java.util.List.of()));

    java.lang.reflect.Method method =
        MistralProvider.class.getDeclaredMethod("extractResponse", String.class);
    method.setAccessible(true);
    String result = (String) method.invoke(provider, jsonResponse);

    assertEquals("No response from Mistral.", result);
  }

  @Test
  void constructor_default_shouldUseEmptyKey() {
    MistralProvider p = new MistralProvider();
    assertEquals("mistral", p.getName());
  }

  @Test
  void constructor_withCustomModel_shouldAcceptModel() {
    MistralProvider p = new MistralProvider("key", "mistral-medium-latest");
    assertEquals("mistral", p.getName());
  }

  @Test
  void inheritance_shouldExtendAbstractHttpProvider() {
    assertTrue(provider instanceof AbstractHttpProvider);
    assertTrue(provider instanceof AIProvider);
  }

  @Test
  void httpIntegration_shouldReturnGeneratedText() throws Exception {
    String responseBody =
        objectMapper.writeValueAsString(
            Map.of(
                "choices",
                java.util.List.of(
                    Map.of("message", Map.of("content", "Mocked Mistral response")))));
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setBody(responseBody)
            .addHeader("Content-Type", "application/json"));

    StepVerifier.create(provider.generate("test prompt"))
        .expectNext("Mocked Mistral response")
        .verifyComplete();

    RecordedRequest request = mockWebServer.takeRequest();
    assertEquals("Bearer mistral-test-key", request.getHeader("Authorization"));
    assertEquals("POST", request.getMethod());
  }

  @Test
  void httpIntegration_shouldHandleHttpError() throws Exception {
    mockWebServer.enqueue(
        new MockResponse().setResponseCode(429).setBody("{\"error\": \"Rate limited\"}"));

    StepVerifier.create(provider.generate("test")).expectError(RuntimeException.class).verify();
  }
}
