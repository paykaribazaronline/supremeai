package com.supremeai.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IdeaDetectionServiceTest {

  private IdeaDetectionService service;

  @BeforeEach
  void setUp() {
    service = new IdeaDetectionService();
  }

  @Test
  void analyze_shouldDetectBrilliantIdea() {
    String text = "This is a novel and disruptive algorithm that will monetize data streams.";

    IdeaDetectionService.IdeaAnalysisResult result = service.analyze(text, "user-1");

    assertNotNull(result);
    assertTrue(result.getScore() >= 20);
    assertTrue(result.isBrilliant());
  }

  @Test
  void analyze_shouldReturnNotIdeaForEmptyText() {
    var result = service.analyze("", "user-1");

    assertNotNull(result);
    assertFalse(result.isBrilliant());
  }

  @Test
  void analyze_shouldReturnNotIdeaForNullText() {
    var result = service.analyze(null, "user-1");

    assertNotNull(result);
    assertFalse(result.isBrilliant());
  }
}
