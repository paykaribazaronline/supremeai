package com.supremeai.service.analysis;

import java.util.Map;
import java.util.HashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class FixPromptTemplates {

  /**
   * Template storage decoupled from source.
   * Logic now requires initialization from external configuration or Knowledge
   * Base.
   */
  private final Map<String, String> templates = new HashMap<>();

  public FixPromptTemplate getTemplate(String category) {
    String template = templates.getOrDefault(category, "");
    if (template.isEmpty()) {
      log.warn("No prompt template found for category: {}. Ensure Knowledge Base is seeded.", category);
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
