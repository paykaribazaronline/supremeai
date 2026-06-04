package com.supremeai.intelligence.human;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** Unit tests for ClarificationOption. Tests option text, hint, and toString format. */
class ClarificationOptionTest {

  @Test
  void testConstructor() {
    ClarificationOption option = new ClarificationOption("JWT Auth", "Stateless");

    assertEquals("JWT Auth", option.getOptionText());
    assertEquals("Stateless", option.getHint());
  }

  @Test
  void testToString_format() {
    ClarificationOption option = new ClarificationOption("OAuth2", "Social login");

    String str = option.toString();

    assertEquals("- OAuth2\n  (Hint: Social login)", str);
  }

  @Test
  void testToString_withEmptyHint() {
    ClarificationOption option = new ClarificationOption("Option", "");

    String str = option.toString();

    assertEquals("- Option\n  (Hint: )", str);
  }

  @Test
  void testToString_withNullHint() {
    ClarificationOption option = new ClarificationOption("Option", null);

    String str = option.toString();

    assertEquals("- Option\n  (Hint: null)", str);
  }

  @Test
  void testToString_withSpecialCharacters() {
    ClarificationOption option =
        new ClarificationOption("JWT (JSON)", "Best for React/Angular apps");

    String str = option.toString();

    assertTrue(str.contains("JWT (JSON)"));
    assertTrue(str.contains("React/Angular"));
  }

  @Test
  void testGetters() {
    ClarificationOption option = new ClarificationOption("Text", "Hint");

    assertEquals("Text", option.getOptionText());
    assertEquals("Hint", option.getHint());
  }

  @Test
  void testLongText() {
    String longText =
        "This is a very long option text that should still work correctly without any issues";
    String longHint = "This hint is also quite long and provides detailed explanation";

    ClarificationOption option = new ClarificationOption(longText, longHint);

    assertEquals(longText, option.getOptionText());
    assertEquals(longHint, option.getHint());
    assertTrue(option.toString().contains(longText));
  }
}
