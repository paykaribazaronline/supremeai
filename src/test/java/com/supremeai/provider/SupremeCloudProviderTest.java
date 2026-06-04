package com.supremeai.provider;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class SupremeCloudProviderTest {

  @Test
  public void testGetRequestUrl_Standard() {
    SupremeCloudProvider provider =
        new SupremeCloudProvider("key", "test_provider", "model", "https://api.example.com");
    assertEquals("https://api.example.com/v1/chat/completions", provider.getRequestUrl());
  }

  @Test
  public void testGetRequestUrl_TrailingSlash() {
    SupremeCloudProvider provider =
        new SupremeCloudProvider("key", "test_provider", "model", "https://api.example.com/");
    assertEquals("https://api.example.com/v1/chat/completions", provider.getRequestUrl());
  }

  @Test
  public void testGetRequestUrl_HfInference() {
    // Mock HF inference URL
    SupremeCloudProvider provider =
        new SupremeCloudProvider(
            "key", "hf_test", "model", "https://api-inference.huggingface.co/models/test");
    assertEquals("https://api-inference.huggingface.co/models/test", provider.getRequestUrl());
  }

  @Test
  public void testGetCapabilities() {
    SupremeCloudProvider provider =
        new SupremeCloudProvider("key", "render_test", "model_x", "https://render.com");
    var caps = provider.getCapabilities();
    assertEquals("render_test", caps.get("name"));
    assertEquals("model_x", caps.get("model"));
    assertEquals("cloud-native", caps.get("type"));
  }

  @Test
  public void testExtractResponse_Standard() throws Exception {
    SupremeCloudProvider provider =
        new SupremeCloudProvider("key", "test", "model", "http://test.com");
    String json = "{\"choices\": [{\"message\": {\"content\": \"Hello!\"}}]}";
    assertEquals("Hello!", provider.extractResponse(json));
  }

  @Test
  public void testExtractResponse_HfList() throws Exception {
    SupremeCloudProvider provider =
        new SupremeCloudProvider(
            "key", "hf_test", "model", "https://api-inference.huggingface.co/models/test");
    String json = "[{\"generated_text\": \"Generated content\"}]";
    assertEquals("Generated content", provider.extractResponse(json));
  }

  @Test
  public void testExtractResponse_HfMap() throws Exception {
    SupremeCloudProvider provider =
        new SupremeCloudProvider(
            "key", "hf_test", "model", "https://api-inference.huggingface.co/models/test");
    String json = "{\"generated_text\": \"Map content\"}";
    assertEquals("Map content", provider.extractResponse(json));
  }
}
