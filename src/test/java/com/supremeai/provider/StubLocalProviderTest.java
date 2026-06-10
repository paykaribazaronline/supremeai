package com.supremeai.provider;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StubLocalProviderTest {

  private StubLocalProvider provider;

  @BeforeEach
  void setUp() {
    provider = new StubLocalProvider();
    // Manually trigger init() to load core_knowledge.json
    provider.init();
  }

  @Test
  void testInitLoadsKnowledgeBase() {
    int topicCount = (Integer) provider.getCapabilities().get("topicCount");
    assertTrue(
        topicCount > 0,
        "Knowledge base should not be empty after init. Check core_knowledge.json.");
  }

  @Test
  void testGenerateRealResponseFactual() {
    // Tests if it can successfully match a known keyword (e.g., flutter) from core_knowledge.json
    String response = provider.generate("what is flutter mobile development").block();

    assertNotNull(response);
    assertTrue(
        response.toLowerCase().contains("flutter")
            || response.toLowerCase().contains("ui toolkit"));
  }

  @Test
  void testGenerateRealResponseFallback() {
    String response = provider.generate("how to bake a chocolate cake").block();
    assertNotNull(response);
    assertTrue(
        response.contains("help you with")
            || response.contains("knowledge")
            || response.contains("SupremeAI"));
  }
}
