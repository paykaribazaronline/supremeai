package com.supremeai.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AIProviderServiceTest {

  private AIProviderService service;
  private Map<String, String> initialKeys;

  @BeforeEach
  void setUp() {
    initialKeys = new HashMap<>();
    initialKeys.put("openai", "sk-test1");
    initialKeys.put("anthropic", "sk-ant-test1");
    service = new AIProviderService(initialKeys);
  }

  @Test
  void getActiveKey_shouldReturnKeyForExistingProvider() {
    String key = service.getActiveKey("openai");
    assertEquals("sk-test1", key);
  }

  @Test
  void getActiveKey_shouldReturnNullForNonExistingProvider() {
    String key = service.getActiveKey("unknown");
    assertNull(key);
  }

  @Test
  void rotateKey_shouldRotateToNextKey() {
    // Add another key for openai
    service.addKey("openai", "sk-test2");

    String firstKey = service.getActiveKey("openai");
    assertEquals("sk-test1", firstKey);

    String rotatedKey = service.rotateKey("openai");
    assertEquals("sk-test2", rotatedKey);

    String nextKey = service.getActiveKey("openai");
    assertEquals("sk-test2", nextKey);
  }

  @Test
  void rotateKey_shouldReturnNullWhenNoKeys() {
    String rotatedKey = service.rotateKey("unknown");
    assertNull(rotatedKey);
  }

  @Test
  void markKeyAsExhausted_shouldMarkKeyAndRotateWhenActive() {
    service.addKey("openai", "sk-test2");

    service.markKeyAsExhausted("openai", "sk-test1");

    String activeKey = service.getActiveKey("openai");
    assertEquals("sk-test2", activeKey);
  }

  @Test
  void markKeyAsExhausted_shouldReturnNullWhenAllKeysExhausted() {
    service.markKeyAsExhausted("openai", "sk-test1");

    String activeKey = service.getActiveKey("openai");
    assertNull(activeKey);
  }

  @Test
  void resetExhaustedKeys_shouldAllowReuseOfPreviouslyExhaustedKeys() {
    service.markKeyAsExhausted("openai", "sk-test1");

    // Should return null since key is exhausted
    String activeKey = service.getActiveKey("openai");
    assertNull(activeKey);

    // Reset exhausted keys
    service.resetExhaustedKeys("openai");

    // Should return the key again
    activeKey = service.getActiveKey("openai");
    assertEquals("sk-test1", activeKey);
  }

  @Test
  void addKey_shouldAddKeyToExistingProvider() {
    service.addKey("openai", "sk-test2");

    String firstKey = service.getActiveKey("openai");
    assertEquals("sk-test1", firstKey);

    service.rotateKey("openai");

    String secondKey = service.getActiveKey("openai");
    assertEquals("sk-test2", secondKey);
  }

  @Test
  void addKey_shouldAddKeyToNewProvider() {
    service.addKey("newprovider", "newkey");

    String key = service.getActiveKey("newprovider");
    assertNotNull(key);
    assertEquals("newkey", key);
  }

  @Test
  void rotateKey_shouldWrapAroundWhenAtEndOfList() {
    service.addKey("openai", "sk-test2");
    service.addKey("openai", "sk-test3");

    // Start at first key
    assertEquals("sk-test1", service.getActiveKey("openai"));

    // Rotate to second
    service.rotateKey("openai");
    assertEquals("sk-test2", service.getActiveKey("openai"));

    // Rotate to third
    service.rotateKey("openai");
    assertEquals("sk-test3", service.getActiveKey("openai"));

    // Rotate back to first (wrap around)
    service.rotateKey("openai");
    assertEquals("sk-test1", service.getActiveKey("openai"));
  }
}
