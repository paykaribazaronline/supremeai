package com.supremeai.provider;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** Test CodeGeeX4Provider configuration and instantiation */
class CodeGeeX4ProviderTest {

  @Test
  void codegeex4ProviderCanBeCreated() {
    // Test that provider can be created with a dummy key
    CodeGeeX4Provider provider = new CodeGeeX4Provider("test-key");
    assertNotNull(provider);
    assertEquals("codegeex4", provider.getName());
  }

  @Test
  void codegeex4ProviderCapabilities() {
    CodeGeeX4Provider provider = new CodeGeeX4Provider("test-key");
    var capabilities = provider.getCapabilities();

    assertNotNull(capabilities);
    assertEquals("CodeGeeX4 (智谱AI)", capabilities.get("name"));
    assertEquals("CodeGeeX4", capabilities.get("provider"));
    assertTrue(capabilities.containsKey("models"));
    assertTrue(capabilities.containsKey("languages"));
    assertTrue(capabilities.containsKey("context"));
  }

  @Test
  void codegeex4ProviderWithCustomModel() {
    CodeGeeX4Provider provider = new CodeGeeX4Provider("test-key", "codegeex-4-lite");
    assertNotNull(provider);
    assertEquals("codegeex4", provider.getName());
  }

  @Test
  void codegeex4ProviderInheritance() {
    CodeGeeX4Provider provider = new CodeGeeX4Provider("test-key");
    // Verify it extends AbstractHttpProvider
    assertTrue(provider instanceof AbstractHttpProvider);
    // Verify it implements AIProvider
    assertTrue(provider instanceof AIProvider);
  }
}
