package com.supremeai.intelligence.human;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for RequirementClarification. Tests clarification building, option management, and
 * chat response formatting.
 */
class RequirementClarificationTest {

  @Test
  void testConstructor() {
    RequirementClarification clarification = new RequirementClarification("What do you need?");

    assertNotNull(clarification);
    assertEquals("What do you need?", clarification.getClarifyingQuestion());
    assertNotNull(clarification.getOptions());
    assertTrue(clarification.getOptions().isEmpty());
  }

  @Test
  void testAddOption() {
    RequirementClarification clarification = new RequirementClarification("Choose one:");

    clarification.addOption("Option A", "Hint A");
    clarification.addOption("Option B", "Hint B");

    assertEquals(2, clarification.getOptions().size());
    assertEquals("Option A", clarification.getOptions().get(0).getOptionText());
    assertEquals("Hint A", clarification.getOptions().get(0).getHint());
  }

  @Test
  void testAddMultipleOptions() {
    RequirementClarification clarification = new RequirementClarification("Question");

    for (int i = 1; i <= 5; i++) {
      clarification.addOption("Option " + i, "Hint " + i);
    }

    assertEquals(5, clarification.getOptions().size());
  }

  @Test
  void testBuildChatResponse_singleOption() {
    RequirementClarification clarification = new RequirementClarification("Pick one:");
    clarification.addOption("Option 1", "First hint");

    String response = clarification.buildChatResponse();

    assertNotNull(response);
    assertTrue(response.contains("Pick one:"));
    assertTrue(response.contains("1. Option 1"));
    assertTrue(response.contains("[Hint: First hint]"));
    assertTrue(response.contains("Please reply with the number"));
  }

  @Test
  void testBuildChatResponse_multipleOptions() {
    RequirementClarification clarification = new RequirementClarification("Choose:");
    clarification.addOption("A", "Hint A");
    clarification.addOption("B", "Hint B");
    clarification.addOption("C", "Hint C");

    String response = clarification.buildChatResponse();

    assertTrue(response.contains("1. A"));
    assertTrue(response.contains("2. B"));
    assertTrue(response.contains("3. C"));
    assertTrue(response.contains("Hint A"));
    assertTrue(response.contains("Hint B"));
    assertTrue(response.contains("Hint C"));
  }

  @Test
  void testBuildChatResponse_emptyOptions() {
    RequirementClarification clarification = new RequirementClarification("No options?");

    String response = clarification.buildChatResponse();

    assertNotNull(response);
    assertTrue(response.contains("No options?"));
    assertTrue(response.endsWith("Please reply with the number"));
  }

  @Test
  void testGetOptions_returnsModifiableList() {
    RequirementClarification clarification = new RequirementClarification("Test");
    clarification.addOption("Option", "Hint");

    var options = clarification.getOptions();

    assertNotNull(options);
    assertEquals(1, options.size());
    assertEquals("Option", options.get(0).getOptionText());
  }

  @Test
  void testBuildChatResponse_lineBreaks() {
    RequirementClarification clarification = new RequirementClarification("Question");
    clarification.addOption("Option", "Hint");

    String response = clarification.buildChatResponse();

    assertTrue(response.contains("\n\n"));
    assertTrue(response.split("\n").length > 3);
  }

  @Test
  void testChainingAddOption() {
    RequirementClarification clarification = new RequirementClarification("Pick");

    clarification.addOption("A", "A hint");
    clarification.addOption("B", "B hint");

    assertEquals(2, clarification.getOptions().size());
  }

  @Test
  void testClarificationWithLongText() {
    RequirementClarification clarification =
        new RequirementClarification(
            "This is a very long question that contains multiple sentences and details about what the user needs to understand before making a choice");
    clarification.addOption(
        "Option with very long text that should still be formatted correctly",
        "Very detailed hint explaining exactly what this option means and why someone might choose it");

    String response = clarification.buildChatResponse();

    assertNotNull(response);
    assertTrue(response.contains("Option with very long text"));
    assertTrue(response.contains("Very detailed hint"));
  }
}
