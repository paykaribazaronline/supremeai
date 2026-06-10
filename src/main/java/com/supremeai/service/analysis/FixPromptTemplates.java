package com.supremeai.service.analysis;

import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class FixPromptTemplates {

  /**
   * Template storage decoupled from source. Logic now requires initialization from external
   * configuration or Knowledge Base.
   */
  private final Map<String, String> templates = new HashMap<>();

  public FixPromptTemplates() {
    templates.put(
        "SECURITY",
        "You are a security expert. Analyze the following code for security vulnerabilities. Provide EXPLANATION, FIXED_CODE, and CONFIDENCE.");
    templates.put(
        "QUALITY",
        "You are a code quality expert. Review the following code for quality issues. Provide EXPLANATION, FIXED_CODE, and CONFIDENCE.");
    templates.put(
        "DEPENDENCIES",
        "You are a dependency management expert. Analyze the following dependencies. Provide EXPLANATION, FIXED_CODE, and CONFIDENCE.");
    templates.put(
        "ARCHITECTURE",
        "You are a software architect. Review the following architecture. Provide EXPLANATION, FIXED_CODE, and CONFIDENCE.");
    templates.put(
        "DEFAULT",
        "You are an expert code reviewer. Analyze the following code. Provide EXPLANATION, FIXED_CODE, and CONFIDENCE.");
  }

  public FixPromptTemplate getTemplate(String category) {
    String template = templates.getOrDefault(category, templates.get("DEFAULT"));
    if (template == null || template.isEmpty()) {
      log.warn(
          "No prompt template found for category: {}. Ensure Knowledge Base is seeded.", category);
    }
    return FixPromptTemplate.builder()
        .templateName(category)
        .template(template)
        .outputFormat("EXPLANATION: ...\nFIXED_CODE: ...\nCONFIDENCE: ...")
        .build();
  }

  public FixPromptTemplate getDefaultTemplate() {
    return getTemplate("DEFAULT");
  }
}
