package com.supremeai.intelligence.human;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for RequirementAnalyzer. Tests vagueness detection and clarification generation for
 * login, bugs, and app building scenarios.
 */
class RequirementAnalyzerTest {

  private final RequirementAnalyzer analyzer = new RequirementAnalyzer();

  @Test
  void testAnalyzeRequirement_vagueLoginPrompt() {
    String prompt = "login";
    RequirementClarification clarification = analyzer.analyzeRequirement(prompt);

    assertNotNull(clarification, "Should return clarification for vague login prompt");
    assertTrue(clarification.getClarifyingQuestion().contains("login"));
    assertEquals(3, clarification.getOptions().size(), "Should have 3 authentication options");
  }

  @Test
  void testAnalyzeRequirement_vagueBugReport() {
    String prompt = "fix bug";
    RequirementClarification clarification = analyzer.analyzeRequirement(prompt);

    assertNotNull(clarification, "Should return clarification for vague bug report");
    assertTrue(clarification.getClarifyingQuestion().toLowerCase().contains("bug"));
    assertEquals(3, clarification.getOptions().size(), "Should have 3 bug type options");
  }

  @Test
  void testAnalyzeRequirement_vagueAppBuild() {
    String prompt = "build app";
    RequirementClarification clarification = analyzer.analyzeRequirement(prompt);

    assertNotNull(clarification, "Should return clarification for vague app build request");
    assertTrue(clarification.getClarifyingQuestion().toLowerCase().contains("app"));
    assertEquals(3, clarification.getOptions().size(), "Should have 3 app purpose options");
  }

  @Test
  void testAnalyzeRequirement_detailedLoginPrompt() {
    String prompt =
        "I need to implement JWT-based authentication with Spring Security for a React frontend, including refresh tokens and role-based access control";
    RequirementClarification clarification = analyzer.analyzeRequirement(prompt);

    assertNull(clarification, "Detailed prompt should not require clarification");
  }

  @Test
  void testAnalyzeRequirement_detailedBugReport() {
    String prompt =
        "NullPointerException at line 45 in UserService.java when calling userRepository.findById with null id";
    RequirementClarification clarification = analyzer.analyzeRequirement(prompt);

    assertNull(
        clarification, "Detailed bug report with exception should not require clarification");
  }

  @Test
  void testAnalyzeRequirement_detailedAppDescription() {
    String prompt =
        "Build an e-commerce application with product catalog, shopping cart, Stripe payment integration, and order management system";
    RequirementClarification clarification = analyzer.analyzeRequirement(prompt);

    assertNull(clarification, "Detailed app description should not require clarification");
  }

  @Test
  void testAnalyzeRequirement_shortPromptWithoutKeywords() {
    String prompt = "hello";
    RequirementClarification clarification = analyzer.analyzeRequirement(prompt);

    assertNull(
        clarification, "Short prompt without trigger keywords should not require clarification");
  }

  @Test
  void testAnalyzeRequirement_caseInsensitiveMatching() {
    String prompt = "LOGIN";
    RequirementClarification clarification = analyzer.analyzeRequirement(prompt);

    assertNotNull(clarification, "Case-insensitive matching should work for LOGIN");
  }

  @Test
  void testAnalyzeRequirement_loginWithDetailedContext() {
    String prompt = "I need a login system with OAuth2 and Google authentication";
    RequirementClarification clarification = analyzer.analyzeRequirement(prompt);

    assertNull(clarification, "Login with OAuth2 mention is detailed enough");
  }

  @Test
  void testAnalyzeRequirement_bugWithExceptionDetails() {
    String prompt = "fix the error: ArrayIndexOutOfBoundsException at index 5";
    RequirementClarification clarification = analyzer.analyzeRequirement(prompt);

    assertNull(clarification, "Bug with specific exception should not require clarification");
  }

  @Test
  void testLoginClarificationOptionsContent() {
    String prompt = "make a login page";
    RequirementClarification clarification = analyzer.analyzeRequirement(prompt);

    assertNotNull(clarification);
    var options = clarification.getOptions();

    assertTrue(options.get(0).getOptionText().contains("JWT"));
    assertTrue(options.get(1).getOptionText().contains("Session"));
    assertTrue(options.get(2).getOptionText().contains("OAuth2"));

    assertTrue(
        options.get(0).getHint().toLowerCase().contains("mobile")
            || options.get(0).getHint().toLowerCase().contains("spa"));
    assertTrue(options.get(1).getHint().toLowerCase().contains("web"));
    assertTrue(options.get(2).getHint().toLowerCase().contains("conversion"));
  }

  @Test
  void testBugClarificationOptionsContent() {
    String prompt = "fix bug";
    RequirementClarification clarification = analyzer.analyzeRequirement(prompt);

    assertNotNull(clarification);
    var options = clarification.getOptions();

    assertEquals(3, options.size());
    assertTrue(options.stream().anyMatch(o -> o.getOptionText().contains("crash")));
    assertTrue(options.stream().anyMatch(o -> o.getOptionText().contains("data")));
    assertTrue(options.stream().anyMatch(o -> o.getOptionText().contains("UI")));
  }

  @Test
  void testAppBuildClarificationOptionsContent() {
    String prompt = "build an app";
    RequirementClarification clarification = analyzer.analyzeRequirement(prompt);

    assertNotNull(clarification);
    var options = clarification.getOptions();

    assertTrue(
        options.stream().anyMatch(o -> o.getOptionText().toLowerCase().contains("e-commerce")));
    assertTrue(
        options.stream().anyMatch(o -> o.getOptionText().toLowerCase().contains("dashboard")));
    assertTrue(options.stream().anyMatch(o -> o.getOptionText().toLowerCase().contains("social")));
  }

  @Test
  void testBuildChatResponseFormat() {
    String prompt = "login";
    RequirementClarification clarification = analyzer.analyzeRequirement(prompt);
    String response = clarification.buildChatResponse();

    assertNotNull(response);
    assertTrue(response.contains("1."));
    assertTrue(response.contains("2."));
    assertTrue(response.contains("3."));
    assertTrue(response.contains("Hint"));
    assertTrue(response.endsWith("Please reply with the number"));
  }

  @Test
  void testAnalyzeRequirement_edgeCase_emptyString() {
    String prompt = "";
    RequirementClarification clarification = analyzer.analyzeRequirement(prompt);

    assertNull(clarification);
  }

  @Test
  void testAnalyzeRequirement_edgeCase_nullInput() {
    RequirementClarification clarification = analyzer.analyzeRequirement(null);

    assertNull(clarification);
  }
}
