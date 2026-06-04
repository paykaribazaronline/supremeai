package com.supremeai.agent;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.supremeai.agentorchestration.Question;
import com.supremeai.provider.AIProvider;
import com.supremeai.provider.AIProviderFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;

class GPublishAgentTest {

  @Mock private AIProviderFactory providerFactory;

  @Mock private AIProvider aiProvider;

  @InjectMocks private GPublishAgent gPublishAgent;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    lenient().when(providerFactory.getDefaultProvider()).thenReturn(aiProvider);
  }

  @Test
  void testAnalyzePublishingRequirements_returnsQuestions() {
    String jsonResponse =
        "[{\"key\":\"publish\",\"text\":\"Test question\",\"priority\":\"HIGH\"}]";
    when(providerFactory.getProvider("groq")).thenReturn(aiProvider);
    when(aiProvider.generate(anyString())).thenReturn(Mono.just(jsonResponse));

    List<Question> result = gPublishAgent.analyzePublishingRequirements("Test requirement");

    assertNotNull(result);
    assertFalse(result.isEmpty());
    assertEquals("publish", result.get(0).getKey());
  }

  @Test
  void testAnalyzePublishingRequirements_returnsDefaultsOnException() {
    when(providerFactory.getProvider("groq")).thenReturn(aiProvider);
    when(aiProvider.generate(anyString()))
        .thenReturn(Mono.error(new RuntimeException("API error")));

    List<Question> result = gPublishAgent.analyzePublishingRequirements("Test requirement");

    assertNotNull(result);
    assertFalse(result.isEmpty());
  }

  @Test
  void testCreatePublishingPlan_iosPlatform() {
    Map<String, String> config = new HashMap<>();
    config.put("iosDistribution", "TestFlight");

    Map<String, String> plan = gPublishAgent.createPublishingPlan("iOS", config);

    assertEquals("iOS", plan.get("platform"));
    assertEquals("App Store", plan.get("store"));
    assertEquals("Xcode", plan.get("buildTool"));
    assertEquals("TestFlight", plan.get("distributionMethod"));
  }

  @Test
  void testCreatePublishingPlan_androidPlatform() {
    Map<String, String> config = new HashMap<>();
    config.put("androidDistribution", "Google Play");

    Map<String, String> plan = gPublishAgent.createPublishingPlan("Android", config);

    assertEquals("Android", plan.get("platform"));
    assertEquals("Google Play Store", plan.get("store"));
    assertEquals("Gradle", plan.get("buildTool"));
  }

  @Test
  void testCreatePublishingPlan_webPlatform() {
    Map<String, String> config = new HashMap<>();
    config.put("webHosting", "Netlify");

    Map<String, String> plan = gPublishAgent.createPublishingPlan("web", config);

    assertEquals("Web", plan.get("platform"));
    assertEquals("Netlify", plan.get("hosting"));
    assertEquals("Vite", plan.get("buildTool"));
  }

  @Test
  void testCreatePublishingPlan_desktopPlatform() {
    Map<String, String> config = new HashMap<>();
    config.put("desktopDistribution", "Direct Download");

    Map<String, String> plan = gPublishAgent.createPublishingPlan("desktop", config);

    assertEquals("Desktop", plan.get("platform"));
    assertEquals("Electron Builder", plan.get("packaging"));
    assertEquals("Direct Download", plan.get("distribution"));
  }

  @Test
  void testCreatePublishingPlan_unknownPlatform() {
    Map<String, String> config = new HashMap<>();

    Map<String, String> plan = gPublishAgent.createPublishingPlan("unknown", config);

    assertTrue(plan.containsKey("error"));
  }

  @Test
  void testAnalyzePublishingRequirements_includesBengaliPrompt() {
    when(providerFactory.getProvider("groq")).thenReturn(aiProvider);
    when(aiProvider.generate(anyString())).thenReturn(Mono.just("[]"));

    gPublishAgent.analyzePublishingRequirements("Requirement");

    verify(aiProvider)
        .generate(
            argThat(prompt -> prompt.contains("পাবলিশিং") && prompt.contains("ডিপ্লয়মেন্ট")));
  }
}
