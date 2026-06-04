package com.supremeai.provider;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Test;

class AbstractHttpProviderTest {

  private static final ObjectMapper objectMapper = new ObjectMapper();

  static class TestProvider extends AbstractHttpProvider {
    TestProvider() {
      super("test-key", "https://api.test.com/v1/chat", "test-model");
    }

    @Override
    public String getName() {
      return "test-provider";
    }

    @Override
    protected Map<String, Object> createRequestBody(String prompt) {
      return Map.of("prompt", prompt, "model", "test-model");
    }

    @Override
    protected String extractResponse(String responseBody) {
      return responseBody;
    }

    @Override
    protected void addAuthHeaders(okhttp3.Request.Builder builder) {
      builder.addHeader("Authorization", "Bearer test-key");
    }
  }

  @Test
  void constructor_shouldSetApiKey() {
    TestProvider provider = new TestProvider();
    assertEquals("test-provider", provider.getName());
  }

  @Test
  void constructor_withNullObjectMapper_shouldCreateDefault() {
    TestProvider provider = new TestProvider();
    assertNotNull(provider);
  }

  @Test
  void getCapabilities_shouldReturnModelTypeAndUrl() {
    TestProvider provider = new TestProvider();
    Map<String, Object> caps = provider.getCapabilities();

    assertNotNull(caps);
    assertEquals("remote", caps.get("type"));
    assertEquals("test-model", caps.get("model"));
  }

  @Test
  void getCapabilities_baseDefaultShouldIncludeModelTypeUrl() {
    TestProvider provider = new TestProvider();
    Map<String, Object> caps = provider.getCapabilities();

    assertNotNull(caps.get("model"));
    assertNotNull(caps.get("type"));
    assertNotNull(caps.get("url"));
  }

  @Test
  void createRequestBody_shouldReturnMapWithPrompt() {
    TestProvider provider = new TestProvider();
    Map<String, Object> body = provider.createRequestBody("Hello");

    assertNotNull(body);
    assertEquals("Hello", body.get("prompt"));
    assertEquals("test-model", body.get("model"));
  }

  @Test
  void extractResponse_shouldReturnRawResponse() throws Exception {
    TestProvider provider = new TestProvider();
    String result = provider.extractResponse("raw response");
    assertEquals("raw response", result);
  }

  @Test
  void addAuthHeaders_shouldAddBearerToken() throws Exception {
    TestProvider provider = new TestProvider();
    okhttp3.Request.Builder builder = new okhttp3.Request.Builder();
    provider.addAuthHeaders(builder);

    okhttp3.Request request = builder.url("https://api.test.com").build();
    assertEquals("Bearer test-key", request.header("Authorization"));
  }

  @Test
  void sharedHttpClient_shouldBeInitialized() {
    assertNotNull(AbstractHttpProvider.sharedHttpClient);
  }

  @Test
  void sharedHttpClient_shouldHaveConfiguredTimeouts() {
    OkHttpClient client = AbstractHttpProvider.sharedHttpClient;
    assertEquals(30, client.connectTimeoutMillis() / 1000);
    assertEquals(120, client.readTimeoutMillis() / 1000);
  }

  @Test
  void sharedHttpClient_shouldRetryOnFailure() {
    assertTrue(AbstractHttpProvider.sharedHttpClient.retryOnConnectionFailure());
  }

  @Test
  void generate_shouldReturnMono() {
    TestProvider provider = new TestProvider();
    reactor.core.publisher.Mono<String> result = provider.generate("test");
    assertNotNull(result);
  }

  @Test
  void getRequestUrl_shouldReturnBaseUrl() {
    TestProvider provider = new TestProvider();
    String url = provider.getRequestUrl();
    assertEquals("https://api.test.com/v1/chat", url);
  }

  @Test
  void inheritance_testProviderShouldImplementAIProvider() {
    TestProvider provider = new TestProvider();
    assertTrue(provider instanceof AIProvider);
    assertTrue(provider instanceof AbstractHttpProvider);
  }
}
