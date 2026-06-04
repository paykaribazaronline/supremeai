package com.supremeai.service.analysis;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class FixPromptTemplates {

  private static final String DEFAULT_TEMPLATE =
      """
        You are an expert code reviewer. Fix the following issue:

        File: {filePath}
        Line: {lineNumber}
        Severity: {severity}
        Category: {category}
        Issue: {findingMessage}
        Suggestion: {suggestion}

        Code snippet:
        ```
        {codeSnippet}
        ```

        Provide the fixed code that resolves this issue. Output format:
        EXPLANATION: [brief explanation of the fix]
        FIXED_CODE: [complete fixed code snippet that replaces the original]
        CONFIDENCE: [0.0-1.0 confidence score]
        """;

  private static final String SECURITY_TEMPLATE =
      """
        You are a security expert. Fix the following security vulnerability:

        File: {filePath}
        Line: {lineNumber}
        Severity: {severity}
        Category: {category}
        Vulnerability: {findingMessage}
        Recommendation: {suggestion}

        Vulnerable code:
        ```
        {codeSnippet}
        ```

        Provide a secure fix that eliminates this vulnerability. Output format:
        EXPLANATION: [brief security fix explanation]
        FIXED_CODE: [complete secure code snippet]
        CONFIDENCE: [0.0-1.0]
        """;

  private static final String QUALITY_TEMPLATE =
      """
        You are a code quality expert. Improve the following code:

        File: {filePath}
        Line: {lineNumber}
        Severity: {severity}
        Category: {category}
        Issue: {findingMessage}
        Recommendation: {suggestion}

        Current code:
        ```
        {codeSnippet}
        ```

        Provide improved code that addresses the quality issue. Output format:
        EXPLANATION: [brief improvement explanation]
        FIXED_CODE: [improved code snippet]
        CONFIDENCE: [0.0-1.0]
        """;

  private static final String DEPENDENCY_TEMPLATE =
      """
        You are a dependency management expert. Fix the following dependency issue:

        File: {filePath}
        Line: {lineNumber}
        Severity: {severity}
        Category: {category}
        Issue: {findingMessage}
        Recommendation: {suggestion}

        Current dependency declaration:
        ```
        {codeSnippet}
        ```

        Provide the corrected dependency configuration. Output format:
        EXPLANATION: [brief explanation]
        FIXED_CODE: [corrected dependency declaration]
        CONFIDENCE: [0.0-1.0]
        """;

  private static final String ARCHITECTURE_TEMPLATE =
      """
        You are a software architect. Fix the following architectural issue:

        File: {filePath}
        Line: {lineNumber}
        Severity: {severity}
        Category: {category}
        Issue: {findingMessage}
        Recommendation: {suggestion}

        Current code:
        ```
        {codeSnippet}
        ```

        Provide refactored code that resolves the architectural issue. Output format:
        EXPLANATION: [brief refactoring explanation]
        FIXED_CODE: [refactored code snippet]
        CONFIDENCE: [0.0-1.0]
        """;

  private final Map<String, String> templates =
      Map.of(
          "DEFAULT", DEFAULT_TEMPLATE,
          "SECURITY", SECURITY_TEMPLATE,
          "QUALITY", QUALITY_TEMPLATE,
          "DEPENDENCIES", DEPENDENCY_TEMPLATE,
          "ARCHITECTURE", ARCHITECTURE_TEMPLATE);

  public FixPromptTemplate getTemplate(String category) {
    String template = templates.getOrDefault(category, DEFAULT_TEMPLATE);
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
