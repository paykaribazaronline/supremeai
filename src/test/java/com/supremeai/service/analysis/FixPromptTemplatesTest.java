package com.supremeai.service.analysis;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class FixPromptTemplatesTest {

  private final FixPromptTemplates templates = new FixPromptTemplates();

  @Test
  void testGetTemplateSecurity() {
    FixPromptTemplate template = templates.getTemplate("SECURITY");
    assertNotNull(template);
    assertEquals("SECURITY", template.getTemplateName());
    assertTrue(template.getTemplate().contains("security expert"));
  }

  @Test
  void testGetTemplateQuality() {
    FixPromptTemplate template = templates.getTemplate("QUALITY");
    assertNotNull(template);
    assertTrue(template.getTemplate().contains("code quality expert"));
  }

  @Test
  void testGetTemplateDependencies() {
    FixPromptTemplate template = templates.getTemplate("DEPENDENCIES");
    assertNotNull(template);
    assertTrue(template.getTemplate().contains("dependency management expert"));
  }

  @Test
  void testGetTemplateArchitecture() {
    FixPromptTemplate template = templates.getTemplate("ARCHITECTURE");
    assertNotNull(template);
    assertTrue(template.getTemplate().contains("software architect"));
  }

  @Test
  void testGetTemplateDefault() {
    FixPromptTemplate template = templates.getTemplate("UNKNOWN");
    assertNotNull(template);
    assertTrue(template.getTemplate().contains("expert code reviewer"));
  }

  @Test
  void testTemplateRender() {
    FixPromptTemplate template = templates.getTemplate("SECURITY");
    FixPromptTemplate.FixContext context =
        FixPromptTemplate.FixContext.builder()
            .filePath("src/Main.java")
            .lineNumber(42)
            .findingMessage("SQL Injection vulnerability")
            .suggestion("Use parameterized queries")
            .codeSnippet("stmt.executeQuery(\"SELECT * FROM users WHERE id=\" + id)")
            .severity("CRITICAL")
            .category("SQL_INJECTION")
            .language("java")
            .build();

    String rendered = template.render(context);
    assertTrue(rendered.contains("src/Main.java"));
    assertTrue(rendered.contains("42"));
    assertTrue(rendered.contains("SQL Injection vulnerability"));
    assertTrue(rendered.contains("Use parameterized queries"));
    assertTrue(rendered.contains("EXPLANATION:"));
    assertTrue(rendered.contains("FIXED_CODE:"));
    assertTrue(rendered.contains("CONFIDENCE:"));
  }
}
