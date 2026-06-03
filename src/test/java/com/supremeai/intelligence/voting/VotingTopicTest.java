package com.supremeai.intelligence.voting;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for VotingTopic.
 * Tests topic creation, getters, and data storage.
 */
class VotingTopicTest {

    @Test
    void testConstructor() {
        VotingTopic topic = new VotingTopic("123", "SECURITY", "context", "question");

        assertEquals("123", topic.getTopicId());
        assertEquals("SECURITY", topic.getCategory());
        assertEquals("context", topic.getContext());
        assertEquals("question", topic.getQuestionToAsk());
    }

    @Test
    void testGetters() {
        VotingTopic topic = new VotingTopic("id-456", "PERFORMANCE", "some code context", "review this");

        assertEquals("id-456", topic.getTopicId());
        assertEquals("PERFORMANCE", topic.getCategory());
        assertEquals("some code context", topic.getContext());
        assertEquals("review this", topic.getQuestionToAsk());
    }

    @Test
    void testConstructor_withNullValues() {
        VotingTopic topic = new VotingTopic(null, null, null, null);

        assertNull(topic.getTopicId());
        assertNull(topic.getCategory());
        assertNull(topic.getContext());
        assertNull(topic.getQuestionToAsk());
    }

    @Test
    void testConstructor_withEmptyStrings() {
        VotingTopic topic = new VotingTopic("", "", "", "");

        assertEquals("", topic.getTopicId());
        assertEquals("", topic.getCategory());
        assertEquals("", topic.getContext());
        assertEquals("", topic.getQuestionToAsk());
    }

    @Test
    void testConstructor_longStrings() {
        String longId = "uuid-very-long-unique-identifier-string";
        String longContext = "This is a very long code context that contains many characters and details about the proposed change";
        String longQuestion = "This is a very long and detailed question that asks about multiple aspects of the code including security, performance, and maintainability concerns";

        VotingTopic topic = new VotingTopic(longId, "GENERAL_REVIEW", longContext, longQuestion);

        assertEquals(longId, topic.getTopicId());
        assertTrue(longContext.length() > 50);
        assertTrue(longQuestion.length() > 50);
    }

    @Test
    void testTopicImmutability() {
        VotingTopic topic = new VotingTopic("id", "CAT", "ctx", "q");

        // Getters should return same values
        assertEquals("id", topic.getTopicId());
        assertEquals("CAT", topic.getCategory());
        assertEquals("ctx", topic.getContext());
        assertEquals("q", topic.getQuestionToAsk());
    }

    @Test
    void testDifferentCategories() {
        String[] categories = {"ARCHITECTURE", "SECURITY", "MAINTAINABILITY", "PERFORMANCE", "GENERAL_REVIEW"};

        for (String category : categories) {
            VotingTopic topic = new VotingTopic("id", category, "ctx", "q");
            assertEquals(category, topic.getCategory());
        }
    }

    @Test
    void testEquality_sameValues() {
        VotingTopic t1 = new VotingTopic("123", "SECURITY", "ctx", "q");
        VotingTopic t2 = new VotingTopic("123", "SECURITY", "ctx", "q");

        // Not overriding equals() so these are different objects but same data
        assertNotSame(t1, t2);
        assertEquals(t1.getTopicId(), t2.getTopicId());
    }

    @Test
    void testHashCode_consistency() {
        VotingTopic topic = new VotingTopic("123", "SECURITY", "ctx", "q");
        int hash1 = topic.hashCode();
        int hash2 = topic.hashCode();

        assertEquals(hash1, hash2);
    }

    @Test
    void testToString_format() {
        VotingTopic topic = new VotingTopic("abc123", "SECURITY", "code snippet", "review question");

        String str = topic.toString();

        assertTrue(str.contains("abc123"));
        assertTrue(str.contains("SECURITY"));
    }
}
