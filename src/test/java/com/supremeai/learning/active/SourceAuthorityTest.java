package com.supremeai.learning.active;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** Unit tests for SourceAuthority enum. Tests authority weights and source name mapping. */
class SourceAuthorityTest {

  @Test
  void testAllAuthoritiesHavePositiveWeight() {
    for (SourceAuthority authority : SourceAuthority.values()) {
      assertTrue(authority.getWeight() > 0, authority + " weight should be positive");
      assertTrue(authority.getWeight() <= 1.0, authority + " weight should be <= 1.0");
    }
  }

  @Test
  void testWeightsOrder() {
    // Official docs should be highest weight
    assertEquals(1.0, SourceAuthority.OFFICIAL_DOCS.getWeight());
    // Blogs less than GitHub
    assertTrue(SourceAuthority.GITHUB.getWeight() > SourceAuthority.BLOGS.getWeight());
    // StackOverflow > Wikipedia
    assertTrue(SourceAuthority.STACK_OVERFLOW.getWeight() > SourceAuthority.WIKIPEDIA.getWeight());
  }

  @Test
  void testFromSourceName_officialDocumentation() {
    SourceAuthority auth = SourceAuthority.fromSourceName("Official Documentation");
    assertEquals(SourceAuthority.OFFICIAL_DOCS, auth);
  }

  @Test
  void testFromSourceName_github() {
    SourceAuthority auth = SourceAuthority.fromSourceName("github.com/repo");
    assertEquals(SourceAuthority.GITHUB, auth);
  }

  @Test
  void testFromSourceName_gitlab() {
    SourceAuthority auth = SourceAuthority.fromSourceName("GitLab Docs");
    assertEquals(SourceAuthority.GITHUB, auth);
  }

  @Test
  void testFromSourceName_stackOverflow() {
    SourceAuthority auth = SourceAuthority.fromSourceName("stackoverflow.com/questions");
    assertEquals(SourceAuthority.STACK_OVERFLOW, auth);
  }

  @Test
  void testFromSourceName_wikipedia() {
    SourceAuthority auth = SourceAuthority.fromSourceName("Wikipedia Article");
    assertEquals(SourceAuthority.WIKIPEDIA, auth);
  }

  @Test
  void testFromSourceName_blogs() {
    SourceAuthority auth = SourceAuthority.fromSourceName("Medium Blog Post");
    assertEquals(SourceAuthority.BLOGS, auth);
  }

  @Test
  void testFromSourceName_forumsDefault() {
    SourceAuthority auth = SourceAuthority.fromSourceName("some-random-forum.com");
    assertEquals(SourceAuthority.FORUMS, auth);
  }

  @Test
  void testFromSourceName_nullReturnsForums() {
    SourceAuthority auth = SourceAuthority.fromSourceName(null);
    assertEquals(SourceAuthority.FORUMS, auth);
  }

  @Test
  void testFromSourceName_emptyString() {
    SourceAuthority auth = SourceAuthority.fromSourceName("");
    assertEquals(SourceAuthority.FORUMS, auth);
  }

  @Test
  void testFromSourceName_caseInsensitive() {
    SourceAuthority auth1 = SourceAuthority.fromSourceName("GITHUB");
    assertEquals(SourceAuthority.GITHUB, auth1);

    SourceAuthority auth2 = SourceAuthority.fromSourceName("GiThUb");
    assertEquals(SourceAuthority.GITHUB, auth2);
  }

  @Test
  void testAllSourcesAreMapped() {
    String[] testSources = {
      "docs.microsoft.com",
      "developer.android.com",
      "docs.spring.io",
      "github.com",
      "gitlab.com",
      "stackoverflow.com",
      "stackexchange.com",
      "wikipedia.org",
      "medium.com",
      "wordpress.com"
    };

    for (String source : testSources) {
      SourceAuthority auth = SourceAuthority.fromSourceName(source);
      assertNotNull(auth);
      assertNotEquals(
          SourceAuthority.FORUMS,
          auth,
          "Expected authority for " + source + " should not be default FORUMS");
    }
  }

  @Test
  void testWeightsReflectReliability() {
    // Ensure ordering reflects authority: official > github/so > wikipedia > blogs > forums
    assertTrue(SourceAuthority.OFFICIAL_DOCS.getWeight() >= SourceAuthority.GITHUB.getWeight());
    assertTrue(SourceAuthority.GITHUB.getWeight() >= SourceAuthority.STACK_OVERFLOW.getWeight());
    assertTrue(SourceAuthority.STACK_OVERFLOW.getWeight() >= SourceAuthority.WIKIPEDIA.getWeight());
    assertTrue(SourceAuthority.WIKIPEDIA.getWeight() >= SourceAuthority.BLOGS.getWeight());
    assertTrue(SourceAuthority.BLOGS.getWeight() >= SourceAuthority.FORUMS.getWeight());
  }

  @Test
  void testFromSourceName_withSpacesAndSpecialChars() {
    SourceAuthority auth = SourceAuthority.fromSourceName("Google Cloud Documentation");
    assertEquals(SourceAuthority.OFFICIAL_DOCS, auth);
  }
}
