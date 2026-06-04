package com.supremeai.service.analysis;

import java.util.regex.Pattern;

/**
 * Represents a single pattern rule. Manual implementation to bypass Lombok issues in build
 * environment.
 */
public class PatternRule {
  private String category;
  private Pattern pattern;
  private String message;
  private String suggestion;

  public PatternRule() {}

  public PatternRule(String category, Pattern pattern, String message, String suggestion) {
    this.category = category;
    this.pattern = pattern;
    this.message = message;
    this.suggestion = suggestion;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public Pattern getPattern() {
    return pattern;
  }

  public void setPattern(Pattern pattern) {
    this.pattern = pattern;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getSuggestion() {
    return suggestion;
  }

  public void setSuggestion(String suggestion) {
    this.suggestion = suggestion;
  }

  public static PatternRuleBuilder builder() {
    return new PatternRuleBuilder();
  }

  public static class PatternRuleBuilder {
    private String category;
    private Pattern pattern;
    private String message;
    private String suggestion;

    public PatternRuleBuilder category(String category) {
      this.category = category;
      return this;
    }

    public PatternRuleBuilder pattern(Pattern pattern) {
      this.pattern = pattern;
      return this;
    }

    public PatternRuleBuilder message(String message) {
      this.message = message;
      return this;
    }

    public PatternRuleBuilder suggestion(String suggestion) {
      this.suggestion = suggestion;
      return this;
    }

    public PatternRule build() {
      return new PatternRule(category, pattern, message, suggestion);
    }
  }
}
