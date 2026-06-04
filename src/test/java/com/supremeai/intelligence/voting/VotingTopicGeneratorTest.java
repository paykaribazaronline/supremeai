package com.supremeai.intelligence.voting;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for VotingTopicGenerator. Tests topic generation for different change types and
 * default fallback.
 */
class VotingTopicGeneratorTest {

  private final VotingTopicGenerator generator = new VotingTopicGenerator();

  @Test
  void testGenerateTopicForMajorChange_databaseMigration() {
    String code = "ALTER TABLE users ADD COLUMN email VARCHAR(255);";

    VotingTopic topic = generator.generateTopicForMajorChange("DATABASE_MIGRATION", code);

    assertNotNull(topic);
    assertEquals("ARCHITECTURE", topic.getCategory());
    assertTrue(topic.getContext().contains("ALTER TABLE"));
    assertTrue(topic.getQuestionToAsk().toLowerCase().contains("migration"));
    assertTrue(topic.getQuestionToAsk().toLowerCase().contains("safe"));
  }

  @Test
  void testGenerateTopicForMajorChange_authLogicChange() {
    String code = "if (token == null || !verify(token)) { reject(); }";

    VotingTopic topic = generator.generateTopicForMajorChange("AUTH_LOGIC_CHANGE", code);

    assertNotNull(topic);
    assertEquals("SECURITY", topic.getCategory());
    assertTrue(topic.getQuestionToAsk().toLowerCase().contains("owasp"));
    assertTrue(topic.getQuestionToAsk().toLowerCase().contains("vulnerabilit"));
  }

  @Test
  void testGenerateTopicForMajorChange_newThirdPartyLib() {
    String code = "implementation 'com.google.guava:guava:31.1-jre'";

    VotingTopic topic = generator.generateTopicForMajorChange("NEW_THIRD_PARTY_LIB", code);

    assertNotNull(topic);
    assertEquals("MAINTAINABILITY", topic.getCategory());
    assertTrue(topic.getQuestionToAsk().toLowerCase().contains("maintained"));
    assertTrue(topic.getQuestionToAsk().toLowerCase().contains("bloat"));
  }

  @Test
  void testGenerateTopicForMajorChange_generalReviewFallback() {
    String code = "public void complexAlgorithm() { /* complex logic */ }";

    VotingTopic topic = generator.generateTopicForMajorChange("UNKNOWN_CHANGE", code);

    assertNotNull(topic);
    assertEquals("GENERAL_REVIEW", topic.getCategory());
    assertTrue(topic.getQuestionToAsk().toLowerCase().contains("edge cases"));
    assertTrue(topic.getQuestionToAsk().toLowerCase().contains("performance"));
  }

  @Test
  void testGenerateTopicForMajorChange_nullCodeSnippet() {
    VotingTopic topic = generator.generateTopicForMajorChange("DATABASE_MIGRATION", null);

    assertNotNull(topic);
    assertTrue(topic.getContext().contains("null"));
  }

  @Test
  void testGenerateTopicForMajorChange_emptyCodeSnippet() {
    VotingTopic topic = generator.generateTopicForMajorChange("AUTH_LOGIC_CHANGE", "");

    assertNotNull(topic);
    assertTrue(topic.getContext().isEmpty() || topic.getContext().contains(""));
  }

  @Test
  void testGenerateTopicForMajorChange_topicIdIsUnique() {
    String code = "some code";
    VotingTopic topic1 = generator.generateTopicForMajorChange("GENERAL_REVIEW", code);
    VotingTopic topic2 = generator.generateTopicForMajorChange("GENERAL_REVIEW", code);

    assertNotEquals(topic1.getTopicId(), topic2.getTopicId());
  }

  @Test
  void testGenerateTopicForMajorChange_databaseFlywayMigration() {
    String code = "V1__Add_email_column.sql";
    VotingTopic topic = generator.generateTopicForMajorChange("DATABASE_MIGRATION", code);

    assertNotNull(topic);
    assertTrue(
        topic.getQuestionToAsk().toLowerCase().contains("flyway")
            || topic.getQuestionToAsk().toLowerCase().contains("liquibase"));
  }

  @Test
  void testGenerateTopicForMajorChange_authWithJWT() {
    String code = "Jwts.parser().setSigningKey(secret).parseClaimsJws(token)";
    VotingTopic topic = generator.generateTopicForMajorChange("AUTH_LOGIC_CHANGE", code);

    assertTrue(topic.getQuestionToAsk().toLowerCase().contains("jwt"));
  }
}
